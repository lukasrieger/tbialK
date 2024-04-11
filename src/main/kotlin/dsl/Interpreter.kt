package dsl

import kotlinx.coroutines.flow.update
import state.drawCards


/**
 * An [Interpreter] defines the execution rules by which an arbitrary [Action] is to be
 * reduced to a result value of [A].
 */
fun interface Interpreter<C, H, A> {

    context(C, H)
    @Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
    suspend fun Action<A>.execute(): A
}

fun <A> defaultClientInterpreter() =
    object : Interpreter<GameSessionContext, ClientHandlerContext, A> {
        context(GameSessionContext, ClientHandlerContext)
        override tailrec suspend fun Action<A>.execute(): A =
            when (this) {
                is ClientAction ->
                    when (this) {
                        is Action.CheckStumbling ->
                            next(gameState.value.getStumblingCardForCurrentPlayer()).execute()

                        is Action.GetGameState ->
                            next(gameState.value).execute()

                        is Action.PlayCard ->
                            next(server { playCard() }).execute()

                        is Action.SelectAttack ->
                            next(client { selectAttack() }).execute()

                        is Action.SelectDiscardCards ->
                            next(client { selectDiscardCards(amount) }).execute()

                        is Action.SelectPlayer ->
                            next(client { selectPlayer() }).execute()

                        is Action.SelectResponse ->
                            next(client { selectResponse() }).execute()
                    }

                is ServerAction ->
                    when (this) {
                        is Action.AttackPlayer ->
                            next(server { attackPlayer(target, bug) }).execute()

                        is Action.DefendAttack ->
                            next(server { defendAttack(defence) }).execute()

                        is Action.Discard -> {
                            server { discard(toDiscard) }
                            next().execute()
                        }

                        is Action.DrawCards ->
                            next(server { drawCards() }).execute()

                        is Action.HandleStumbling ->
                            next(server { handleStumbling(via) }).execute()
                    }

                is Action.Done -> result
            }
    }

fun <A> defaultServerInterpreter() =
    object : Interpreter<ServerGameSessionContext, ServerHandlerContext, A> {
        context(ServerGameSessionContext, ClientHandlerContext)
        override suspend fun Action<A>.execute(): A =
            when (this) {
                is ServerAction ->
                    when (this) {
                        is Action.AttackPlayer -> TODO()
                        is Action.DefendAttack -> TODO()
                        is Action.Discard -> TODO()
                        is Action.DrawCards -> {
                            val (drawn, nextState) = drawCards(1)(gameState.value)
                            gameState.update { nextState }
                            next(drawn.let { it[0] to it[1] }).execute()
                        }

                        is Action.HandleStumbling -> TODO()
                    }

                is Action.Done -> result
                else -> error("Server can't handle client actions.")
            }

    }
