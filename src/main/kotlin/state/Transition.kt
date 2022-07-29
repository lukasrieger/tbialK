package state

typealias Guard<S, E> = (S, E) -> Boolean

operator fun <S, E> Guard<S, E>.plus(other: Guard<S, E>): Guard<S, E> = { state, event ->
    this(state, event) && other(state, event)
}

fun <S, E> emptyGuard(): Guard<S, E> = { _, _ -> true }
data class Transition<S, V, E>(val source: V, val target: V, val event: E, val guard: Guard<S, E> = emptyGuard())
