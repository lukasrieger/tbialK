package common

import kotlinx.serialization.Serializable

@Serializable
enum class Turn {
    Stumbling,
    Draw,
    PlayCards
}
