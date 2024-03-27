package common.cards

import kotlinx.serialization.Serializable

@Serializable
enum class Excuse(
    override val title: String,
    override val description: String
) : Card {

    WorksForMe("Works For Me!", "Fends of bug report"),
    ItsAFeature("It's a Feature", "Fends of bug report"),
    ImNotResponsible("I'm not Responsible", "Fends of bug report");

    override val kind: CardKind = CardKind.Excuse

    companion object {
        val playingSet = entries.flatMap { excuse -> List(4) { excuse } }
    }
}
