package common.cards

import kotlinx.serialization.Serializable

@Serializable
enum class Solution(
    override val title: String,
    override val description: String
) : Card {

    Coffee("Coffee", "+1 mental health"),
    CodeFixSession("Code+Fix Session", "+1 mental health"),
    RegularExpression("I know regular expressions", "+1 mental health");

    override val kind: CardKind = CardKind.Solution

    companion object {
        val playingSet: List<Card> = entries.flatMap { solution -> List(2) { solution } }
    }
}
