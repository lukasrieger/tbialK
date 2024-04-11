package common

import common.cards.Card

sealed interface Event {

    val origin: Player

    data class ReactWithCard(override val origin: Player, val victim: Player, val card: Card) : Event
    data class ReactWithoutCard(override val origin: Player, val victim: Player) : Event

    data class NextTurn(override val origin: Player) : Event
    data class DrawCards(override val origin: Player) : Event
}

