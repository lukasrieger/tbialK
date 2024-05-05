package dsl

import arrow.core.nonFatalOrThrow
import common.GameState
import common.Id
import common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

@Serializable
sealed interface Origin {
    @Serializable
    data object Server : Origin

    @Serializable
    data class Player(val player: Id<common.Player>) : Origin
}


@Serializable
sealed interface Command<out A> {
    @Serializable
    data class ActionC<out A>(val origin: Origin, val action: Action<A>) : Command<A>

    @Serializable
    data class Result<out A>(val result: A) : Command<A>
}

data class ServerContext(
    val scope: CoroutineScope,
    val receivers: Map<Id<Player>, ReceiveChannel<Command<*>>>,
    val senders: Map<Id<Player>, SendChannel<Command<*>>>,
    val stateChannel: SendChannel<GameState>,
    val channels: Map<Id<Player>, Pair<SendChannel<Command<*>>, ReceiveChannel<Command<*>>>>,
    val interpreter: Interpreter<ServerGameSessionContext, ServerHandlerContext, Any?>
) : ServerGameSessionContext {

    val receivedResults: Map<Id<Player>, Channel<Any?>> = emptyMap()

    override val gameState: MutableStateFlow<GameState> =
        MutableStateFlow(GameState.EmptyState)

}

data class ClientContext(
    val scope: CoroutineScope,
    val self: Player,
    val receiver: ReceiveChannel<Command<*>> = Channel(),
    val sender: SendChannel<Command<*>> = Channel(),
    val stateChannel: ReceiveChannel<GameState> = Channel(),
    val interpreter: Interpreter<GameSessionContext, ClientHandlerContext, Any?>
) : GameSessionContext {

    val receivedResults: Channel<Any?> = Channel()

    override val gameState: StateFlow<GameState> =
        stateChannel.receiveAsFlow().stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = GameState.EmptyState
        )
}

context(ServerContext)
suspend fun <A> interpretActionClientside(id: Id<Player>, action: Action<A>): A {
    channels.getValue(id).first.send(Command.ActionC(Origin.Server, action))
    val result = receivedResults.getValue(id).receive()

    return result.coerce()
}

context(ClientContext)
suspend fun <A> interpretActionServerside(origin: Id<Player>, action: Action<A>): A {
    sender.send(Command.ActionC(Origin.Player(origin), action))
    val result = receivedResults.receive()

    return result.coerce()
}

context(ClientContext)
suspend fun <A> interpretActionClientside(action: Action<A>): A {
    delay(0)
    TODO()
}


context(ClientContext)
fun buildClientCallbacks() = object : ClientHandlerContext {
    override suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A =
        interpretActionServerside(self.id, action { block() })

    override suspend fun <A> client(block: suspend ActionBuilder<A>.() -> A): A =
        interpretActionClientside(action { block() })
}

context(ServerContext)
fun buildServerCallbacks() = object : ServerHandlerContext {
    override suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A =
        with(Origin.Server) {
            interpreter.run { action { block() }.execute() }.coerce()
        }

    override suspend fun <A> client(id: Id<Player>, block: suspend ActionBuilder<A>.() -> A): A =
        interpretActionClientside(id, action { block() })

    override suspend fun <A> client(id: Id<Player>, action: Action<A>): A =
        interpretActionClientside(id, action)

}

context(ClientContext, ClientHandlerContext)
suspend fun globalClientHandler() {
    with(interpreter) {
        while (true) {
            select {
                receiver.onReceive {
                    with(Origin.Server) {
                        when (it) {
                            is Command.ActionC<*> -> sender.send(Command.Result(it.action.execute()))
                            is Command.Result<*> -> receivedResults.send(it.result)
                        }
                    }
                }
            }
        }
    }
}


context(ServerContext, ServerHandlerContext, Interpreter<ServerGameSessionContext, ServerHandlerContext, Any?>)
suspend fun globalServerHandler() {
    gameState.onEach { stateChannel.send(it) }

    while (true) {
        select {
            channels.forEach { (id, channels) ->
                val (sender, receiver) = channels
                receiver.onReceive { command ->
                    with(Origin.Player(id)) {
                        when (command) {
                            is Command.ActionC -> sender.send(Command.Result(command.action.execute()))
                            is Command.Result -> receivedResults[id]?.send(id to command.result)
                        }
                    }
                }
            }
        }
    }
}


@Suppress("UNCHECKED_CAST")
private fun <A> Any?.coerce() = try {
    this as A
} catch (e: Throwable) {
    val nonFatal = e.nonFatalOrThrow()
    error("Could not read result value, got error instead: ${nonFatal.message}")
}