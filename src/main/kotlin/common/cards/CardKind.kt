package common.cards

import kotlinx.serialization.Serializable

@Serializable
enum class CardKind {
    BugReport, Excuse, Solution, SpecialAction, Role, Character, Stumbling, Ability
}
