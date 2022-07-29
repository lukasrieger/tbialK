package common.cards

enum class BugReport(
    override val title: String,
    override val description: String
) : Card {

    NullPointer("Nullpointer!", "-1 mental health"),
    OffByOne("Off By One!", "-1 mental health"),
    ClassNotFound("Class Not Found!", "-1 mental health"),
    SystemHangs("System Hangs!", "-1 mental health"),
    CoreDump("Core Dump!", "-1 mental health"),
    CustomerHatesUI("Customer hates UI!", "-1 mental health");

    override val kind: CardKind = CardKind.BugReport

    companion object {
        val playingSet: List<Card> = BugReport.values().flatMap { bug -> List(4) { bug } }
    }

}