package common

import common.cards.Card
import state.*

sealed interface Event {

    val origin: Player
    data class ReactWithCard(override val origin: Player, val victim: Player, val card: Card) : Event
    data class ReactWithoutCard(override val origin: Player, val victim: Player) : Event

    data class NextTurn(override val origin: Player) : Event
    data class DrawCards(override val origin: Player) : Event
}

val eventReducer: Reducer<Event, GameState> = { state, event ->
    when (event) {
        is Event.DrawCards -> drawCards(state.indexOfCurrentPlayer, state.heap)
        is Event.NextTurn -> nextPlayerTurn
        is Event.ReactWithCard -> reactWithCard(event.victim, event.card)
        is Event.ReactWithoutCard -> reactWithoutCard(event.victim)
    }(state)
}
