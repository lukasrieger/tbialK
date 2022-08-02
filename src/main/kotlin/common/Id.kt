package common

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Id<T>(val id: Int)
