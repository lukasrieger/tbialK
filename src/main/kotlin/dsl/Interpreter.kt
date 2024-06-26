package dsl

import kotlinx.coroutines.flow.update
import state.drawCards
import state.reactWithoutCard


/**
 * An [Interpreter] defines the execution rules by which an arbitrary [Action] is to be
 * reduced to a result value of [A].
 */
fun interface Interpreter<C, H, A> {

    context(C, H, Origin)
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
        context(ServerGameSessionContext, ServerHandlerContext)
        override suspend fun Action<A>.execute(): A =
            when (this) {
                is ServerAction ->
                    when (this) {
                        is Action.AttackPlayer -> {
                            val outcome = client(target.id) {
                                defendAttack(selectResponse())
                            }

                            when(outcome) {
                                Outcome.Success -> next(outcome).execute()
                                Outcome.Failure -> {
                                    gameState.update(reactWithoutCard(target))
                                    next(outcome).execute()
                                }
                            }
                        }
                        is Action.DefendAttack ->
                            next(if (defence != null) Outcome.Success else Outcome.Failure).execute()
                        is Action.Discard -> {
                            TODO()
                        }
                        is Action.DrawCards -> {
                            val (drawn, nextState) = gameState.value.drawCards()
                            gameState.update { nextState }
                            next(drawn.let { it[0] to it[1] }).execute()
                        }

                        is Action.HandleStumbling -> {
                            val outcome = when(via) {
                                null -> {
//                                    gameState.update(loose)
                                    Outcome.Failure
                                }
                                else -> Outcome.Success
                            }

                            TODO()
                        }
                    }

                is Action.Done -> result
                else -> error("Server can't handle client actions.")
            }

    }
