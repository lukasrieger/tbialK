package dsl

import fp.serrano.inikio.ProgramBuilder

sealed interface Free<K, A> {
    data class Pure<K, A>(val value: A) : Free<K, A>

    data class FlatMapped<K, B, out C>(val c: K, val f: (@UnsafeVariance C) -> Free<K, B>) : Free<K, B>


}

tailrec fun <K, A> Free<K, A>.compile(compiler: Compiler<K, A>): A =
    when(this) {
        is Free.FlatMapped<K, A, Any?> -> {
            val x = compiler.compile(this.c)
            val y = this.f(x)

            y.compile(compiler)
        }

        is Free.Pure -> this.value
    }


sealed interface Action2<out A> {
    data class Put(val value: String) : Action2<Unit>
    data object Read : Action2<String>
}

abstract class FreeBuilder<K, A> : ProgramBuilder<Free<K, A>, A>(
    { result -> Free.Pure(result) }
) {
    internal suspend fun <B> lift(value: K): B = perform { cont ->
        Free.FlatMapped(value, cont)
    }
}

class Action2Builder<A> : FreeBuilder<Action2<*>, A>(){

    suspend fun put(value: String): Unit = lift(Action2.Put(value))

    suspend fun read(): String = lift(Action2.Read)


}

fun <A> action2(block: suspend Action2Builder<A>.() -> A): Free<Action2<*>, A> =
    fp.serrano.inikio.program(Action2Builder(), block)



val freeTest = action2 {
    put("Hello World!")
    val read = read()
    println(read)
    put("Bye World!")
    println("Bye World!")
    read()
}

fun interface Compiler<K, A> {
    fun compile(value: K): Any
}

fun <A> testCompiler() = Compiler<Action2<*>, A> {
    when(it) {
        is Action2.Put -> Unit
        is Action2.Read -> "Hello World!"
    }
}