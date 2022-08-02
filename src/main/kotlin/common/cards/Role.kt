package common.cards

import kotlinx.serialization.Serializable

@Serializable
enum class Role(
    override val title: String,
    override val description: String
) : Card {

    Manager(
        "Manager",
        "Aim: Remove evil code monkeys and consultant"
    ),
    Consultant(
        "Consultant",
        "Aim: Get everyone else fired; Manager last!"
    ),
    HonestDeveloper(
        "Honest Developer",
        "Aim: Get evil code monkeys & consultant fired"

    ),
    EvilCodeMonkey(
        "Evil Code Monkey",
        "Aim: Get the Manager fired."
    );

    override val kind: CardKind = CardKind.Role

    companion object {
        val playingSet = Role.values().flatMap { role ->
            when (role) {
                Manager, Consultant -> listOf(role)
                HonestDeveloper -> List(2) { role }
                EvilCodeMonkey -> List(3) { role }
            }
        }
    }
}
