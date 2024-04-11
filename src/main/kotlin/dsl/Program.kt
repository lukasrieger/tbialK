package dsl

import fp.serrano.inikio.program

interface Program<A> {
    suspend fun <B> ActionBuilder<B>.action(): A
}

fun <A> Program<A>.compile(): Action<A> = program(ActionBuilder()) { action() }


context(ActionBuilder<B>)
suspend operator fun <A, B> Program<A>.invoke(): A = action()

fun <A> program(block: suspend ActionBuilder<*>.() -> A): Program<A> = object : Program<A> {
    override suspend fun <B> ActionBuilder<B>.action(): A = block(this)
}