package common.cards

enum class Character(
    override val title: String,
    override val description: String,
    val ability: String,
    val mentalHealth: Int
) : Card {
    MarkZuckerberg(
        "Mark Zuckerberg",
        "Founder of Facebook",
        "If you lose mental health, take one card from the causer",
        3
    ),
    TomAnderson(
        "Tom Anderson",
        "Founder of MySpace",
        "If you lose mental health, you may take a card from the stack",
        4
    ),
    JeffTaylor("Jeff Taylor", "Founder of monster.com", "No cards left? Take one from the stack", 4),
    LarryPage("Larry Page", "Founder of Google", "When somebody gets fired, you take the cards", 4),
    LarryEllison(
        "Larry Ellison",
        "Founder of Oracle",
        "May take three instead of two cars from the stack, but has to put one back",
        4
    ),
    KentBeck(
        "Kent Beck",
        "Inventor of Extreme Programming",
        "Drop two cards and gain mental health",
        4
    ),
    SteveJobs("Steve Jobs", "Founder of Apple", "Gets a second chance", 4),
    SteveBallmer(
        "Steve Ballmer",
        "Chief Executive Officer of Microsoft",
        "May use bugs as excuses and the way around",
        4
    ),
    LinusTorvalds(
        "Linus Torvalds",
        "Linux Inventor",
        "Bugs found can only be deflected with two excuses",
        4
    ),
    HolierThanThou("Holier than Thou", "Found Everywhere", "Sees everyone with -1 prestige", 4),
    KonradZuse(
        "Konrad Zuse",
        "Built first programmable computer",
        "Is seen with +1 prestige by everybody",
        3
    ),
    BruceSchneier("Bruce Schneier", "Security Guru", "May report an arbitrary number of bugs", 4),
    TerryWeissman(
        "Terry Weissman",
        "Found of Bugzilla",
        "Got a bug? .25 percent chance for delegation",
        4
    ),
    Dummy("Dummy", "Dummy hwile Characters aren't implemented", "Nothing", 4);

    override val kind: CardKind = CardKind.Character

    companion object {
        val playingSet = Character.values().toList()
    }
}
