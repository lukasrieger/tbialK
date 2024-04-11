package state

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.fx.coroutines.parZip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

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
    suspend fun send(event: E): Either<TransitionError, Unit>
}

/**
 * Default FSM implementation, see [stateMachineConfig] and [PartialFSM] for details on how to construct an FSM
 * instance through the provided DSL.
 */
internal class DefaultStateAutomaton<S : Any, E : Any, V : Any>(
    initialState: V,
    initialStoreState: S,
    stateReducer: Reducer<E, S>,
    interceptor: Interceptor<S>,
    transitions: List<Transition.Abstract<S, V, E>>,
    scope: CoroutineScope
) : FSM<S, E, V> {

    private val registeredTransitions: Map<V, Set<Triple<V, KClass<E>, Guard<S, E>>>> =
        buildMap {
            transitions.forEach { (source, target, event, guard) ->
                compute(source) { _, v ->
                    setOf(Triple(target, event, guard)) + (v ?: emptySet())
                }
            }
        }

    private val stateChannel: Channel<Transition.Concrete<S, V, E>> = Channel(capacity = 0)

    override val store: StateStore<E, S> = stateStore(initialStoreState, stateReducer, interceptor, scope)

    override val state: StateFlow<V> = stateChannel
        .receiveAsFlow()
        .map { it.target }
        .stateIn(scope, SharingStarted.Eagerly, initialState)

    private suspend fun transition(event: E) {
        registeredTransitions[state.value]
            ?.find { (_, on) -> on.java.isAssignableFrom(event::class.java) }
            ?.let { (target, _) ->
                parZip(
                    { stateChannel.send(Transition.Concrete(state.value, target, event)) },
                    { store.send(event) }
                ) { _, _ -> }
            }
    }


    override suspend fun send(event: E): Either<TransitionError, Unit> = either {
        ensureTransitionFor(event)
        transition(event)
    }

    /**
     * Ensure that given an event of type [E], there exists a valid transition from the current FSM state
     * [state] to some successor state via event kind [E]. Additionally, ensure that if any potential transition has a
     * guard that accepts the current state and given event.
     */
    context(Raise<TransitionError>)
    private fun ensureTransitionFor(event: E): Unit =
        registeredTransitions[state.value]
            ?.find { (_, on) -> on.java.isAssignableFrom(event::class.java) }
            ?.let { (_, _, guard) ->
                ensure(guard(store.state.value, event)) { TransitionError.GuardFailed(state.value, event) }
            }
            ?: raise(TransitionError.InvalidTransition(state.value, event))
}
