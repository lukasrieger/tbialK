package state

/**
 * Subtypes of this sealed interface each denote a specific error kind that may arise while a finite-state machine is
 * executing.
 */
sealed interface TransitionError {
    /**
     * There exists no valid edge from [state] to some successor state that can be triggered by [event].
     */
    data class InvalidTransition<S, E>(val state: S, val event: E) : TransitionError

    /**
     * There exists a valid transition from [state] to some successor state that can be triggered by [event],
     * but the transition can't be taken because the associated guard has failed.
     */
    data class GuardFailed<S, E>(val state: S, val event: E) : TransitionError
}
