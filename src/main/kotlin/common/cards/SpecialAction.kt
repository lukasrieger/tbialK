package common.cards

import kotlinx.serialization.Serializable

@Serializable
enum class SpecialAction(
    override val title: String,
    override val description: String
) : Card {

    Refactoring("I refactored your Code. Away", "Ignores prestige- Drop one card"),
    Pwnd("Pwnd", "Cede one card. Same or lower prestige required"),
    SystemIntegration("System Integration", "My code is better than yours!"),
    StandupMeeting("Standup Meeting", "The cards are on the table"),
    BoringMeeting("Boring Meeting", "Play bug or lose mental health"),
    CoffeeMachine("Personal Coffee Machine", "Take 2 cards"),
    LANParty("LAN Party", "Mental health for everybody"),
    RedBullDispenser("Red Bull Dispenser", "Take 3 cards"),
    Heisenbug("Heisenbug", "Bugs for everybody!");

    override val kind: CardKind = CardKind.SpecialAction

    companion object {
        val playingSet: List<Card> = entries.flatMap { specialAction ->
            when (specialAction) {
                Refactoring, Pwnd -> List(4) { specialAction }
                SystemIntegration -> List(3) { specialAction }
                StandupMeeting, CoffeeMachine, BoringMeeting -> List(2) { specialAction }
                LANParty, Heisenbug, RedBullDispenser -> listOf(specialAction)
            }
        }
    }
}
