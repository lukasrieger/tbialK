package common

import arrow.optics.optics
import common.cards.Card

@optics
data class GameState(
    val id: Id<GameState>,
    val players: List<Player>,
    val stack: List<Card>,
    val heap: List<Card>,
    val frontCards: Map<Player, List<Card>>,
    val turn: Turn,
    val indexOfCurrentPlayer: Int
) {
    val currentPlayer = players[indexOfCurrentPlayer]

    companion object
}
