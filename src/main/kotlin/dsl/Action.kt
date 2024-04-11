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