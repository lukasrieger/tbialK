package common

import arrow.optics.optics
import common.cards.Card
import common.cards.Character
import common.cards.Role
import java.util.*

@optics
data class Player(
    val id: Id<Player> = Id(UUID.randomUUID().hashCode()),
    val user: User,
    val role: Role,
    val character: Character,
    val mentalHealth: Int = character.mentalHealth,
    val prestige: Int,
    val cards: List<Card>
) {
    companion object
}
