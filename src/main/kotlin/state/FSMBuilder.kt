package state

import arrow.core.curried
import arrow.core.partially5
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * DSL provider to define generic FSM transitions between states of type [V] triggered by events of type [E].
 */

class TransitionBuilder<S : Any, E : Any, V : Any> {
    inner class Edge(val source: V, val target: V)

    private val registeredTransitions: MutableList<Transition.Abstract<S, V, E>> = mutableListOf()

    private var globalGuard = emptyGuard<S, E>()

    /**
     * Global guards are cumulative. This means that consecutive calls to this method with a given guard g2 will
     * *not* override the previous global guard g1, but instead result in a new global guard g3 of shape (g1 && g2)
     */
    fun globalGuard(guard: Guard<S, E>) {
        globalGuard += guard
    }

    /**
     * Defines an edge between two state nodes of type [V].
     * Note that this method alone will *not* register the transition yet, instead returning a new Edge type, that
     * allows the user to specify the transition event type via [via].
     */
    infix fun V.into(target: V): Edge = Edge(this, target)

    /**
     * Via defines the event type for a given FSM transition V1 -> V2. Currently, defining event types is only possible by
     * passing the [KClass] of the event type to this method. If the need arises, this might be extended to support enums.
     */
    infix fun <A : E> Edge.via(event: KClass<A>): Transition.Abstract<S, V, A> {
        val transition = Transition.Abstract(this.source, this.target, event, globalGuard)
        registeredTransitions.add(transition as Transition.Abstract<S, V, E>)

        return transition
    }

    /**
     * This defines a *local* guard for a given abstract [Transition].
     * Note that this will *not* override global guards and instead combine the global guard with this local guard.
     */
    infix fun <A : E> Transition.Abstract<S, V, A>.guard(guard: Guard<S, A>) {
        val transition = registeredTransitions.find { it == this } ?: return
        val guardedTransition = this.copy(guard = transition.guard + guard)

        registeredTransitions[registeredTransitions.indexOf(transition)] =
            guardedTransition as Transition.Abstract<S, V, E>
    }

    fun build() = partialFSM(registeredTransitions)
}

/**
 * An alias for the constructor of [DefaultStateAutomaton], to be used to construct partially applied versions of this
 * function.
 */
typealias FSMConstructor<S, E, V> = (
    initialState: V,
    initialStoreState: S,
    stateReducer: Reducer<E, S>,
    interceptor: Interceptor<S>,
    transitions: List<Transition.Abstract<S, V, E>>,
    scope: CoroutineScope
) -> FSM<S, E, V>

/**
 * A partially constructed finite-state machine with state type [V] and event/action type [E], built via [stateMachineConfig].
 * To construct a fully configured FSM from this configuration, see [invoke].
 */
typealias PartialFSM<S, E, V> = (
    initialState: V,
    initialStoreState: S,
    stateReducer: Reducer<E, S>,
    interceptor: Interceptor<S>,
    scope: CoroutineScope
) -> FSM<S, E, V>

/**
 * An almost fully constructed FSM<S, V, E>, with only the specific [CoroutineScope] to launch the FSM in missing.
 */
typealias FSMProvider<S, V, E> = (scope: CoroutineScope) -> FSM<S, E, V>

private fun <S : Any, E : Any, V : Any> partialFSM(transitions: List<Transition.Abstract<S, V, E>>): PartialFSM<S, E, V> {
    val constructor: FSMConstructor<S, E, V> = ::DefaultStateAutomaton
    return constructor.partially5(transitions)
}

/**
 * DSL entry point to define the model of a finite state machine.
 * The returned [PartialFSM] is a highly re-usable type, allowing the user to define the general FSM model once and
 * apply a wide variety of different initial states, reducers and interceptors depending on the context.
 */
fun <S : Any, E : Any, V : Any> stateMachineConfig(builder: TransitionBuilder<S, E, V>.() -> Unit): PartialFSM<S, E, V> {
    val transitionBuilder = TransitionBuilder<S, E, V>()
    builder(transitionBuilder)

    return transitionBuilder.build()
}

operator fun <S : Any, E : Any, V : Any> PartialFSM<S, E, V>.invoke(
    initialState: V,
    initialStoreState: S,
    stateReducer: Reducer<E, S>,
    interceptor: Interceptor<S> = defaultInterceptor()
): FSMProvider<S, V, E> = curried()(initialState)(initialStoreState)(stateReducer)(interceptor)
