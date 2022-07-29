package state

import common.GameState
import common.Player
import common.cards.Card

internal sealed interface Event {
    data class ReactWithCard(val victim: Player, val card: Card) : Event
    data class ReactWithoutCard(val victim: Player) : Event

    object NextTurn : Event
    object DrawCards : Event
}

internal val eventReducer: Reducer<Event, GameState> = { state, event ->
    when (event) {
        Event.DrawCards -> drawCards(state.indexOfCurrentPlayer, state.heap)
        Event.NextTurn -> nextPlayerTurn
        is Event.ReactWithCard -> reactWithCard(event.victim, event.card)
        is Event.ReactWithoutCard -> reactWithoutCard(event.victim)
    }(state)
}