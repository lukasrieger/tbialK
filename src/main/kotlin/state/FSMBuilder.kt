package state

import kotlinx.coroutines.CoroutineScope

/**
 * DSL provider to define generic FSM transitions between states of type [V] triggered by events of type [E]
 */
class TransitionBuilder<E : Any, V : Any> {
    inner class Edge(val source: V, val target: V)

    private val registeredTransitions: MutableList<Transition<V, E>> = mutableListOf()

    infix fun V.into(target: V): Edge = Edge(this, target)

    infix fun Edge.via(event: E) {
        registeredTransitions.add(Transition(this.source, this.target, event))
    }

    fun build() = PartialFSM(registeredTransitions)
}

/**
 * A partially constructed finite-state machine with state type [V] and event/action type [E], built via [stateMachineConfig].
 * To construct a fully configured FSM from this configuration, see [invoke].
 */
data class PartialFSM<E : Any, V : Any>(val transitions: List<Transition<V, E>>) {

    operator fun <S : Any> invoke(
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

    context(CoroutineScope)
    operator fun <S : Any> invoke(
        initialState: V,
        initialStoreState: S,
        stateReducer: Reducer<E, S>,
        interceptor: Interceptor<S> = defaultInterceptor()
    ): FSM<S, E, V> = DefaultStateAutomaton(
        initialState = initialState,
        initialStoreState = initialStoreState,
        stateReducer = stateReducer,
        interceptor = interceptor,
        transitions = transitions,
        scope = this@CoroutineScope
    )
}

fun <E : Any, V : Any> stateMachineConfig(builder: TransitionBuilder<E, V>.() -> Unit): PartialFSM<E, V> {
    val transitionBuilder = TransitionBuilder<E, V>()
    builder(transitionBuilder)

    return transitionBuilder.build()
}
