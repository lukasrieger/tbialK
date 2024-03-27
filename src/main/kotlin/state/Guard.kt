package state

import common.cards.Card
import fp.serrano.inikio.plugin.InitialStyleDSL

/**
 * A guard is a non-blocking function that given a state S and event E, determines whether a potential transition into
 * some successor state S' would be valid.
 */
typealias Guard<S, E> = (state: S, event: E) -> Boolean

/**
 * Returns a [Guard] g3 that is the logical conjunction of the two input guards: g3 = g1 && g2
 */
operator fun <S, E> Guard<S, E>.plus(other: Guard<S, E>): Guard<S, E> = { state, event ->
    this(state, event) && other(state, event)
}

/**
 * A guard that performs no operation and always returns true right away.
 */
fun <S, E> emptyGuard(): Guard<S, E> = { _, _ -> true }
