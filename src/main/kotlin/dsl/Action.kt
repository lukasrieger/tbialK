package dsl

import common.GameState
import common.Player
import common.cards.BugReport
import common.cards.Card
import fp.serrano.inikio.plugin.InitialStyleDSL
import kotlinx.serialization.Serializable

enum class Outcome { Success, Failure }

typealias PairOf<T> = Pair<T, T>

sealed interface ClientAction
sealed interface ServerAction

@InitialStyleDSL
@Serializable
sealed interface Action<out A> {

    data class Done<out A>(val result: A) : Action<A>

    data class Discard<out A>(
        val toDiscard: List<Card>,
        val next: () -> Action<A>
    ) : Action<A>, ServerAction

    data class CheckStumbling<out A>(
        val next: (Card?) -> Action<A>
    ) : Action<A>, ClientAction

    data class SelectResponse<out A>(
        val next: (Card?) -> Action<A>
    ) : Action<A>, ClientAction

    data class DefendAttack<out A>(
        val defence: Card?,
        val next: (Outcome) -> Action<A>
    ) : Action<A>, ServerAction

    data class HandleStumbling<out A>(
        val via: Card?,
        val next: (Outcome) -> Action<A>
    ) : Action<A>, ServerAction

    data class DrawCards<out A>(
        val next: (PairOf<Card>) -> Action<A>
    ) : Action<A>, ServerAction

    data class SelectDiscardCards<out A>(
        val amount: Int,
        val next: (List<Card>) -> Action<A>
    ) : Action<A>, ClientAction

    data class PlayCard<out A>(
        val next: (Card) -> Action<A>
    ) : Action<A>, ClientAction

    data class SelectPlayer<out A>(
        val next: (Player) -> Action<A>
    ) : Action<A>, ClientAction

    data class SelectAttack<out A>(
        val next: (BugReport) -> Action<A>
    ) : Action<A>, ClientAction

    data class AttackPlayer<out A>(
        val target: Player,
        val bug: BugReport,
        val next: (Outcome) -> Action<A>
    ) : Action<A>, ServerAction

    data class GetGameState<out A>(
        val next: (GameState) -> Action<A>
    ) : Action<A>, ClientAction

}



sealed interface ActionF<out A> {

    data object CheckStumbling : ActionF<Card?>

    data object SelectResponse : ActionF<Card?>

    data object DrawCards : ActionF<PairOf<Card>>

    data object PlayCard : ActionF<Card>

    data object SelectPlayer : ActionF<Player>

    data object SelectAttack : ActionF<BugReport>

    data object GetGameState : ActionF<GameState>

    data class Discard(val cards: List<Card>) : ActionF<Unit>

    data class HandleStumbling(val via: Card?) : ActionF<Outcome>

    data class SelectDiscardCards(val amount: Int) : ActionF<List<Card>>

    data class AttackPlayer(val target: Player, val bug: BugReport) : ActionF<Outcome>

}


class ActionFBuilder<A> : FreeBuilder<ActionF<*>, A>() {
    suspend fun checkStumbling(): Card? = lift(ActionF.CheckStumbling)
    suspend fun selectResponse(): Card? = lift(ActionF.SelectResponse)
    suspend fun drawCards(): PairOf<Card> = lift(ActionF.DrawCards)
    suspend fun playCard(): Card = lift(ActionF.PlayCard)
    suspend fun selectPlayer(): Player = lift(ActionF.SelectPlayer)
    suspend fun selectAttack(): BugReport = lift(ActionF.SelectAttack)
    suspend fun getGameState(): GameState = lift(ActionF.GetGameState)
    suspend fun discard(cards: List<Card>): Unit = lift(ActionF.Discard(cards))
    suspend fun handleStumbling(via: Card?): Outcome = lift(ActionF.HandleStumbling(via))
    suspend fun selectDiscardCards(amount: Int): List<Card> = lift(ActionF.SelectDiscardCards(amount))
    suspend fun attackPlayer(target: Player, bug: BugReport): Outcome = lift(ActionF.AttackPlayer(target, bug))
}

fun <A> actionF(block: suspend ActionFBuilder<A>.() -> A): Free<ActionF<*>, A> =
    fp.serrano.inikio.program(ActionFBuilder(), block)