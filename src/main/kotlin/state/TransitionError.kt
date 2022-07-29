package state

sealed interface TransitionError {
    data class InvalidTransition<S, E>(val state: S, val event: E) : TransitionError
}
