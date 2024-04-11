package dsl


interface HandlerContext {
    suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A

    suspend fun <A> client(block: suspend ActionBuilder<A>.() -> A): A
}