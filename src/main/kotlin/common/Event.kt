package common

import common.cards.Card
import state.*

sealed class Event {

    val from: Player get() = TODO()
    data class ReactWithCard(val victim: Player, val card: Card) : Event()
    data class ReactWithoutCard(val victim: Player) : Event()

    object NextTurn : Event()
    object DrawCards : Event()
}

val eventReducer: Reducer<Event, GameState> = { state, event ->
    when (event) {
        Event.DrawCards -> drawCards(state.indexOfCurrentPlayer, state.heap)
        Event.NextTurn -> nextPlayerTurn
        is Event.ReactWithCard -> reactWithCard(event.victim, event.card)
        is Event.ReactWithoutCard -> reactWithoutCard(event.victim)
    }(state)
}
