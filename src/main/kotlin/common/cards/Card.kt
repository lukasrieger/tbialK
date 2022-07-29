package common.cards

sealed interface Card {
    val kind: CardKind
    val title: String
    val description: String
}