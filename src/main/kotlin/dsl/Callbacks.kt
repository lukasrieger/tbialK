package dsl

import common.Id
import common.Player


interface ClientHandlerContext {
    suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A

    suspend fun <A> client(block: suspend ActionBuilder<A>.() -> A): A
}

interface ServerHandlerContext {
    suspend fun <A> server(block: suspend ActionBuilder<A>.() -> A): A

    suspend fun <A> client(id: Id<Player>, block: suspend ActionBuilder<A>.() -> A): A

    suspend fun <A> client(id: Id<Player>, action: Action<A>): A
}