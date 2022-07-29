package state

data class Transition<V, E>(val source: V, val target: V, val event: E)