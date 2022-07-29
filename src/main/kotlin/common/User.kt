package common

data class User(
    val id: Id<User>,
    val name: String,
    val username: String,
    val password: String
)