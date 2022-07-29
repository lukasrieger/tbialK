package state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.*

/**
 * A reducer is a (possibly) suspending function that computes a successor state [T] based on the current state and an
 * input event [V]
 */
typealias Reducer<V, T> = suspend (state: T, event: V) -> T

/**
 * An interceptor may be used to inspect the produced state and perform (non-blocking) side-effects such as logging.
 */
typealias Interceptor<T> = (state: T) -> Unit

/**
 * A noop interceptor that ignores its input value and returns immediately
 */
internal fun <T> defaultInterceptor(): Interceptor<T> = { }

/**
 * A [StateStore] for a given state [T] and event type [V] is a thin wrapper around a [StateFlow] that additionally
 * provides functions [StateStore.send] and [StateStore.trySend] to continuously update the contained state via events.
 *
 * See [DefaultStateStore], [Reducer] and [Interceptor] for a principled default implementation.
 */
interface StateStore<V, T> {

    /**
     * A [StateFlow] of the current state, beginning with the given initialState of this StateStore.
     */
    val state: StateFlow<T>

    /**
     * Relays the given [event] to the current state, producing a successor state once the event has been handled.
     * See [trySend] for a non-suspending version of this method.
     */
    suspend fun send(event: V)

    /**
     * Non-suspending version of [send].
     * If strict ordering of events is required, this method should not be used.
     */
    fun trySend(event: V): ChannelResult<Unit>
}

/**
 * Default implementation of [StateStore].
 *
 * Successor state computation is handled by [reducer].
 * Each newly computed state is passed to [interceptor] for handling user defined side-effects.
 */
private class DefaultStateStore<V, T>(
    initialState: T,
    reducer: Reducer<V, T>,
    interceptor: Interceptor<T>,
    coroutineScope: CoroutineScope
) : StateStore<V, T> {

    private val eventChannel: Channel<V> = Channel(capacity = 1)

    override val state = eventChannel
        .receiveAsFlow()
        .scan(initialState, reducer)
        .onEach(interceptor)
        .stateIn(coroutineScope, SharingStarted.Eagerly, initialState)

    override suspend fun send(event: V): Unit = eventChannel.send(event)

    override fun trySend(event: V) = eventChannel.trySend(event)
}

/**
 * Given an [initialState], a [Reducer], an [Interceptor] and a [CoroutineScope],
 * return a [StateStore] which computes successor state in the given scope.
 */
internal fun <V, T> stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    interceptor: Interceptor<T>,
    coroutineScope: CoroutineScope
): StateStore<V, T> = DefaultStateStore(initialState, reducer, interceptor, coroutineScope)

/**
 * Convenience function to construct a [StateStore] without an [Interceptor].
 * See [stateStore] for more details.
 */
internal fun <V, T : Any> stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    coroutineScope: CoroutineScope
): StateStore<V, T> = stateStore(initialState, reducer, defaultInterceptor(), coroutineScope)

/**
 * Convenience function to construct a [StateStore] without an [Interceptor] in the context of a [CoroutineScope].
 * See [stateStore] for more details.
 */
internal fun <V, T> CoroutineScope.stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    interceptor: Interceptor<T> = defaultInterceptor()
): StateStore<V, T> = stateStore(initialState, reducer, interceptor, this)
