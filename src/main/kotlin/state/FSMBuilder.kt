package state

import kotlinx.coroutines.CoroutineScope

/**
 * DSL provider to define generic FSM transitions between states of type [V] triggered by events of type [E]
 */
@Suppress("UNCHECKED_CAST")
class TransitionBuilder<S : Any, E : Any, V : Any> {
    inner class Edge(val source: V, val target: V)

    private val registeredTransitions: MutableList<Transition<S, V, E>> = mutableListOf()

    private var globalGuard = emptyGuard<S, E>()

    infix fun V.into(target: V): Edge = Edge(this, target)

    fun globalGuard(guard: Guard<S, E>) {
        globalGuard += guard
    }

    infix fun <A : E> Edge.via(event: A): Transition<S, V, A> {
        val transition = Transition(this.source, this.target, event, globalGuard)
        registeredTransitions.add(transition as Transition<S, V, E>)

        return transition
    }

    infix fun <A : E> Transition<S, V, A>.guard(guard: Guard<S, A>) {
        val transition = registeredTransitions.find { it == this } ?: return
        transition as Transition<S, V, A>
        val guardedTransition = transition.copy(guard = transition.guard + guard)

        registeredTransitions[registeredTransitions.indexOf(transition)] = guardedTransition as Transition<S, V, E>
    }

    fun build() = PartialFSM(registeredTransitions)
}

/**
 * A partially constructed finite-state machine with state type [V] and event/action type [E], built via [stateMachineConfig].
 * To construct a fully configured FSM from this configuration, see [invoke].
 */
data class PartialFSM<S : Any, E : Any, V : Any>(val transitions: List<Transition<S, V, E>>) {

    operator fun invoke(
        initialState: V,
        initialStoreState: S,
        stateReducer: Reducer<E, S>,
        interceptor: Interceptor<S> = defaultInterceptor(),
        scope: CoroutineScope
    ): FSM<S, E, V> = DefaultStateAutomaton(
        initialState = initialState,
        initialStoreState = initialStoreState,
        stateReducer = stateReducer,
        interceptor = interceptor,
        transitions = transitions,
        scope = scope
    )
}

fun <S : Any, E : Any, V : Any> stateMachineConfig(builder: TransitionBuilder<S, E, V>.() -> Unit): PartialFSM<S, E, V> {
    val transitionBuilder = TransitionBuilder<S, E, V>()
    builder(transitionBuilder)

    return transitionBuilder.build()
}
