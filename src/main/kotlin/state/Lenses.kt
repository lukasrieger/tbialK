package state

import arrow.core.compose
import arrow.core.left
import arrow.core.right
import arrow.optics.Every
import arrow.optics.POptional
import arrow.optics.dsl.every
import arrow.optics.dsl.index
import arrow.optics.typeclasses.Index
import common.*
import common.cards.Card

/**
 * Denotes the amount of cards to draw and give to the current player if no other specific amount is given
 */
private const val DEFAULT_DRAW_COUNT = 20

/**
 * A [Index] that can identify a [Player] in a list of players.
 * See [removeCard] as an example on how to use such an index.
 */
private val playerIndex = Index<List<Player>, Player, Player> { player ->
    POptional(
        getOrModify = { it.find(player::equals)?.right() ?: it.left() },
        set = { l, a -> l.map { aa -> if (a.id == aa.id) a else aa } }
    )
}

/**
 * Given a GameState, return a modified GameState with [GameState.indexOfCurrentPlayer] incremented by 1
 */
private val nextPlayerIndex = GameState.indexOfCurrentPlayer.lift { it + 1 }

/**
 * Given a GameState, return a modified GameState with [GameState.turn] transitioned to [Turn.Draw]
 */
private val toDrawState = GameState.turn.lift { Turn.Draw }

/**
 * Given a GameState, return a modified GameState with [GameState.turn] transitioned to [Turn.PlayCards]
 */
private val toPlayState = GameState.turn.lift { Turn.PlayCards }

/**
 * Given a GameState, return a modified GameState with [GameState.turn] transitioned to [Turn.Stumbling]
 */
private val toStumblingState = GameState.turn.lift { Turn.Stumbling }

/**
 * Given a GameState, return a modified GameState with every player's [Player.mentalHealth] increased by 1
 */
private val increaseAllMentalHealth = GameState.players.every(Every.list()).mentalHealth.lift { it + 1 }

/**
 * Given a Player, return a modified Player with [Player.mentalHealth] decreased by 1
 */
private val looseMentalHealth =
    { victim: Player -> GameState.players.index(playerIndex, victim).mentalHealth.lift { it - 1 } }

/**
 * Given a [Card], return a function taking a GameState, that returns a modified GameState with the given card added to the heap
 */
private val addToHeap = { card: Card -> GameState.heap.lift { it + card } }

/**
 * Given the index of a player and a list of cards, return a function taking a GameState, that returns a modified GameState
 * with the card added to the player at the given index.
 */
private val giveCardsByIndex = { index: Int, cards: List<Card> ->
    GameState.players.index(Index.list(), index).cards.lift { it + cards }
}

/**
 * Given an integer, return a function taking a GameState, that returns a modified GameState with the given [count]
 * popped from the [GameState.heap]
 */
private fun popFromHeap(count: Int) = GameState.heap.lift { it.drop(count) }

/**
 * Given a player and a card, return a function taking a GameState, that returns a modified GameState
 * with the given [player] missing the given [card]
 */
private fun removeCard(player: Player, card: Card) =
    GameState.players.index(playerIndex, player).cards.lift { it - card }

/**
 * A composition of the following operations:
 *  - Increment the [GameState.indexOfCurrentPlayer] by 1
 *  - Transfer to the [Turn.Stumbling] state
 */
internal val nextPlayerTurn = nextPlayerIndex compose toStumblingState

/**
 * A composition of the following operations:
 *  - Pop 2 cards from the [heap]
 *  - Give the popped cards to the player at [index]
 *  - Transfer to the [Turn.PlayCards] state
 */
internal fun drawCards(index: Int, heap: List<Card>, amount: Int = DEFAULT_DRAW_COUNT) =
    popFromHeap(amount) compose giveCardsByIndex(index, heap.take(amount)) compose toPlayState

/**
 * A composition of the following operations:
 *  - Subtract 1 mental health from the [victim]
 *  - Transfer to the [Turn.PlayCards] state
 */
internal fun reactWithoutCard(victim: Player) =
    (looseMentalHealth(victim) compose toPlayState)

/**
 * A composition of the following operations:
 *  - Remove the given [card] from the [victim] playing set
 *  - Add the given [card] to the heap
 */
internal fun reactWithCard(victim: Player, card: Card) =
    (removeCard(victim, card) compose addToHeap(card))
