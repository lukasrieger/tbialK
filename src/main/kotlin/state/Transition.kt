package state

import kotlin.reflect.KClass

/**
 * A Transition can be either [Abstract] or [Concrete]. An abstract transition only knows the type of its transition
 * [Abstract.event]. A concrete transition carries a  [Concrete.event] value, allowing the state-machine to perform guard-testing
 * using the concrete instance later on.
 */
sealed interface Transition {

    data class Abstract<S, V, out E : Any>(
        val source: V,
        val target: V,
        val event: KClass<@UnsafeVariance E>,
        val guard: Guard<S, @UnsafeVariance E> = emptyGuard()
    ) : Transition

    data class Concrete<S, V, E : Any>(
        val source: V,
        val target: V,
        val event: E,
        val guard: Guard<S, E> = emptyGuard()
    ) : Transition
}
