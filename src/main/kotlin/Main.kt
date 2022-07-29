
import common.*
import common.cards.Character
import common.cards.PlayingDeck
import common.cards.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import state.Interceptor
import state.stateMachineConfig

private val players = listOf(
    Player(
        user = User(Id(34566), "Lukas", "Lukas", "1234"),
        role = Role.values().random(),
        character = Character.values().random(),
        prestige = 3,
        cards = listOf()
    ),
    Player(
        user = User(Id(87654), "David", "David", "2345678"),
        role = Role.values().random(),
        character = Character.values().random(),
        prestige = 3,
        cards = listOf()
    ),
    Player(
        user = User(Id(3456789), "Hans", "Hans", "98765"),
        role = Role.values().random(),
        character = Character.values().random(),
        prestige = 3,
        cards = listOf()
    )
)

val initialGameState = GameState(
    id = Id(1),
    players = players,
    stack = listOf(),
    heap = PlayingDeck.cards,
    frontCards = mapOf(),
    turn = Turn.Stumbling,
    indexOfCurrentPlayer = 0
)

val stateLoggingInterceptor: Interceptor<GameState> = {
    println("Turn state: ${it.turn}")
    println("Heap size: ${it.heap.size}")
    println("Index of current player: ${it.indexOfCurrentPlayer}")
}

val tbialStateMachineConfig = stateMachineConfig<GameState, _, _> {
    State.Draw into State.Play via Event.DrawCards
    State.Play into State.Draw via Event.NextTurn
}

val tbialStateMachineProvider = { scope: CoroutineScope ->
    tbialStateMachineConfig(
        initialState = State.Draw,
        initialStoreState = initialGameState,
        stateReducer = eventReducer,
        interceptor = stateLoggingInterceptor,
        scope = scope
    )
}

suspend fun main(): Unit = coroutineScope {
    val stateMachine = tbialStateMachineProvider(this)

    launch { stateMachine.store.state.collect() }

    stateMachine.send(Event.DrawCards)
    stateMachine.send(Event.NextTurn)
    stateMachine.send(Event.DrawCards)

    stateMachine.send(
        Event.ReactWithCard(
            stateMachine.store.state.value.currentPlayer,
            stateMachine.store.state.value.currentPlayer.cards.first()
        )
    )

    println("Done...")
}
