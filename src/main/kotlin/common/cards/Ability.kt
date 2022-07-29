package common.cards

enum class Ability(
    override val title: String,
    override val description: String
) : Card {

    BugDelegation("", "Delegates bug report  - .25 percent chance to work"),
    Google("-- previous job --", "2 prestige"),
    Accenture("-- previous job --", "May report several bugs in one round"),
    Microsoft("-- previous job --", "1 prestige"),
    NASA("-- previous job --", "3 prestige"),
    WearsTieAtWork("", "Is seen with +1 prestige by everybody"),
    WearsSunglassesAtWork("", "Sees everybody with -1 prestige");

    override val kind: CardKind = CardKind.Ability


    companion object {
        val playingSet: List<Card> = Ability.values()
            .flatMap { ability ->
                when (ability) {
                    BugDelegation, WearsTieAtWork, Google, Accenture -> List(2) { ability }
                    Microsoft -> List(3) { ability }
                    NASA, WearsSunglassesAtWork -> listOf(ability)
                }
            }
    }

}