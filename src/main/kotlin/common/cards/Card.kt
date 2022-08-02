package common.cards

import kotlinx.serialization.Serializable

@Serializable
sealed interface Card {
    val kind: CardKind
    val title: String
    val description: String
}
