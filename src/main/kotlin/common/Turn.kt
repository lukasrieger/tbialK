package common

import kotlinx.serialization.Serializable

@Serializable
enum class TurnState {
    Stumbling,
    Draw,
    PlayCards
}
