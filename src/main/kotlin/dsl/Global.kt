package dsl

import arrow.core.nonFatalOrThrow
import common.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

@Serializable
sealed interface Command<out A> {
    @Serializable
    data class ActionC<out A>(val action: Action<A>) : Command<A>

    @Serializable
    data class Result<out A>(val result: A) : Command<A>
}


data class ClientContext(
    val scope: CoroutineScope,
    val receiver: ReceiveChannel<Command<*>> = Channel(),
    val sender: SendChannel<Command<*>> = Channel(),
    val stateChannel: ReceiveChannel<GameState> = Channel(),
    val interpreter: Interpreter<Any?>
) : GameSessionContext {

    val receivedResults: Channel<Any?> = Channel()

    override val gameState: StateFlow<GameState> =
        stateChannel.receiveAsFlow().stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = GameState.EmptyState
        )
}


context(ClientContext)
suspend fun <A> interpretActionServerside(action: Action<A>): A {
    sender.send(Command.ActionC(action))
    val result = receivedResults.receive()

    @Suppress("UNCHECKED_CAST")
    return try {
        result as A
    } catch (e: Throwable) {
        val nonFatal = e.nonFatalOrThrow()
        error("Could not read result value, got error instead: ${nonFatal.message}")
    }
}

context(ClientContext)
suspend fun <A> interpretActionClientside(action: Action<A>): A {
    delay(0)
    TODO()
}


context(ClientContext)
fun buildClientCallbacks() = object : HandlerContext {
    override suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A =
        interpretActionServerside(action { block() })

    override suspend fun <A> client(block: suspend ActionBuilder<A>.() -> A): A =
        interpretActionClientside(action { block() })
}

context(ClientContext, HandlerContext)
suspend fun globalClientHandler() {
    with(interpreter) {
        while (true) {
            select {
                receiver.onReceive {
                    when (it) {
                        is Command.ActionC<*> -> sender.send(Command.Result(it.action.execute()))
                        is Command.Result<*> -> receivedResults.send(it.result)
                    }
                }
            }
        }
    }
}