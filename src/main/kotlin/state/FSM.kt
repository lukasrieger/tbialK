package state

import arrow.core.continuations.Effect
import arrow.core.continuations.effect
import arrow.fx.coroutines.parZip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * A finite-state automaton with a state [S], capable of handling events of type [E] transitioning between state nodes
 * of type [V]
 */
interface FSM<S, E, V> {

    /**
     * The underlying state store, which gets updated each time a successor state is reached by an event sent to this
     * FSM via [send].
     */
    val store: StateStore<E, S>

    /**
     * An observable [StateFlow] of the state of this FSM
     */
    val state: StateFlow<V>

    /**
     * Sends an event to this statemachine, causing a transition to the successor state identified by the given [event].
     * If there is no suitable successor state given by the combination of the current state [state] and the given [event],
     * a [TransitionError] is returned instead.
     */
    suspend fun send(event: E): Effect<TransitionError, Unit>

}

/**
 * Default FSM implementation, see [stateMachineConfig] and [PartialFSM.invoke] for details on how to construct an FSM
 * instance through the provided DSL.
 */
internal class DefaultStateAutomaton<S : Any, E : Any, V : Any>(
    initialState: V,
    initialStoreState: S,
    stateReducer: Reducer<E, S>,
    interceptor: Interceptor<S>,
    transitions: List<Transition<V, E>>,
    scope: CoroutineScope
) : FSM<S, E, V> {

    private val registeredTransitions: Map<V, Set<Pair<V, E>>> =
        buildMap {
            transitions.forEach { (source, target, event) ->
                compute(source) { _, v -> setOf(target to event) + (v ?: emptySet()) }
            }
        }

    private val stateChannel: Channel<Transition<V, E>> = Channel(1)

    override val store: StateStore<E, S> = stateStore(initialStoreState, stateReducer, interceptor, scope)

    override val state: StateFlow<V> = stateChannel
        .receiveAsFlow()
        .map { it.target }
        .stateIn(scope, SharingStarted.Lazily, initialState)


    private suspend fun transition(event: E) {
        registeredTransitions[state.value]
            ?.find { (_, on) -> on::class.java.isAssignableFrom(event::class.java) }
            ?.let { (target, _) ->
                parZip(
                    { stateChannel.send(Transition(state.value, target, event)) },
                    { store.send(event) }
                ) { _, _ -> }
            }
    }


    override suspend fun send(event: E): Effect<TransitionError, Unit> = effect {
        ensure(hasTransitionFor(event)) { TransitionError.InvalidTransition(state.value, event) }
        transition(event)
    }


    private fun hasTransitionFor(event: E): Boolean =
        registeredTransitions[state.value]
            ?.any { (_, on) -> on::class.java.isAssignableFrom(event::class.java) }
            ?: false
}

