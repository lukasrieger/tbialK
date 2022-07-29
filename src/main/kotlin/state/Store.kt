package state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.*


typealias Reducer<V, T> = suspend (state: T, event: V) -> T

typealias Interceptor<T> = (state: T) -> Unit

private fun <T> defaultInterceptor(): Interceptor<T> = { }

interface StateStore<V, T> {
    val state: StateFlow<T>

    suspend fun send(event: V)

    fun trySend(event: V): ChannelResult<Unit>
}

private class DefaultStateSore<V, T>(
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

internal fun <V, T : Any> stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    coroutineScope: CoroutineScope
): StateStore<V, T> = DefaultStateSore(initialState, reducer, defaultInterceptor(), coroutineScope)

internal fun <V, T> stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    interceptor: Interceptor<T>,
    coroutineScope: CoroutineScope
): StateStore<V, T> = DefaultStateSore(initialState, reducer, interceptor, coroutineScope)

internal fun <V, T> CoroutineScope.stateStore(
    initialState: T,
    reducer: Reducer<V, T>,
    interceptor: Interceptor<T> = defaultInterceptor()
): StateStore<V, T> = DefaultStateSore(initialState, reducer, interceptor, this)

