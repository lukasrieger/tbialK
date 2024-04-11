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
    val turn: TurnState,
    val indexOfCurrentPlayer: Int
) {
    val currentPlayer = players[indexOfCurrentPlayer]


    fun getStumblingCardForCurrentPlayer(): Card? = TODO()

    companion object {
        val EmptyState = GameState(
            id = Id(-1),
            players = emptyList(),
            stack = emptyList(),
            heap = emptyList(),
            frontCards = emptyMap(),
            turn = TurnState.Draw,
            indexOfCurrentPlayer = -1
        )
    }
}
