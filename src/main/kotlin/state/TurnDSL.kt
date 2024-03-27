package state


import arrow.core.Either
import arrow.core.left
import arrow.core.right
import common.GameState
import common.Id
import common.Player
import common.User
import common.cards.Ability
import common.cards.BugReport
import common.cards.Card
import common.cards.Character
import common.cards.Role
import fp.serrano.inikio.plugin.InitialStyleDSL
import fp.serrano.inikio.program
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow

typealias PairOf<T> = Pair<T, T>

typealias TurnResult = Either<Unit, List<Card>>

enum class Outcome { Success, Failure }

@InitialStyleDSL
sealed interface Action<out A> {

    sealed interface Defence<out A>

    data class Done<out A>(val result: A) : Action<A>

    data class CheckStumbling<out A>(val next: (Card?) -> Action<A>) : Action<A>

    data class SelectResponse<out A>(val next: (Card?) -> Action<A>) : Action<A>, Defence<A>

    data class HandleStumbling<out A>(val via: Card?, val next: (Outcome) -> Action<A>) : Action<A>

    data class DrawCards<out A>(val next: (PairOf<Card>) -> Action<A>) : Action<A>

    data class DiscardCards<out A>(val amount: Int, val next: (List<Card>) -> Action<A>) : Action<A>

    data class PlayCard<out A>(val next: (Card) -> Action<A>) : Action<A>

    data class SelectPlayer<out A>(val next: (Player) -> Action<A>) : Action<A>

    data class SelectAttack<out A>(val next: (BugReport) -> Action<A>) : Action<A>

    data class AttackPlayer<out A>(val target: Player, val bug: BugReport, val next: (Outcome) -> Action<A>) : Action<A>

    data class GetGameState<out A>(val next: (GameState) -> Action<A>) : Action<A>

}

interface GameSessionContext {
    val remote: Channel<Unit>
    val local: Channel<Unit>

    val gameState: StateFlow<GameState>
}

context(GameSessionContext)
tailrec fun <A> Action<A>.execute(): A = when (val action = this) {
    is Action.AttackPlayer -> action.next(Outcome.Success).execute()
    is Action.CheckStumbling -> action.next(null).execute()
    is Action.DiscardCards -> action.next(emptyList()).execute()
    is Action.Done -> action.result
    is Action.DrawCards -> action.next(Ability.entries.take(2).let { (a, b) -> a to b }).execute()
    is Action.GetGameState -> action.next(gameState.value).execute()
    is Action.HandleStumbling -> action.next(Outcome.Success).execute()
    is Action.PlayCard -> action.next(Ability.entries.random()).execute()
    is Action.SelectAttack -> action.next(BugReport.entries.random()).execute()
    is Action.SelectPlayer -> action.next(
        Player(
            user = User(Id(34566), "Lukas", "Lukas", "1234"),
            role = Role.entries.random(),
            character = Character.entries.random(),
            prestige = 3,
            cards = listOf()
        )
    ).execute()

    is Action.SelectResponse -> action.next(null).execute()
}


interface GenAction<A> {

    suspend fun <B> ActionBuilder<B>.action(): A

    fun toAction(): Action<A> = program(ActionBuilder()) { action() }

}

context(ActionBuilder<B>)
suspend operator fun <A, B> GenAction<A>.invoke(): A = action()

fun <A> genAction(block: suspend ActionBuilder<*>.() -> A): GenAction<A> = object : GenAction<A> {
    override suspend fun <B> ActionBuilder<B>.action(): A = block(this)
}


val sequencedAction = genAction {
    attemptToHandleStumblingCard()
    AttackAnotherPlayer()
    finishTurn()
}

val attemptToHandleStumblingCard = genAction {
    when (checkStumbling()) {
        is Card -> handleStumbling(via = selectResponse())
        else -> Outcome.Success
    }
}

val AttackAnotherPlayer = genAction {
    attackPlayer(target = selectPlayer(), bug = selectAttack())
}

val finishTurn = genAction<TurnResult> {
    val currentGameState = getGameState()
    val ownMentalHealth = currentGameState.currentPlayer.mentalHealth
    val currentlyHeldCards = currentGameState.currentPlayer.cards.size

    when {
        currentlyHeldCards > ownMentalHealth -> {
            discardCards(amount = currentlyHeldCards - ownMentalHealth).right()
        }

        else -> Unit.left()
    }
}

