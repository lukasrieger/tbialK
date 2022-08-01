
import common.*
import common.cards.Character
import common.cards.PlayingDeck
import common.cards.Role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import state.Interceptor
import state.invoke
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

val tbialStateMachineConfig = stateMachineConfig {
    globalGuard { state: GameState, event: Event -> state.currentPlayer == event.origin }

    State.Draw into State.Play via Event.DrawCards::class
    State.Play into State.Draw via Event.NextTurn::class
}

val tbialStateMachineProvider = tbialStateMachineConfig(
    initialState = State.Draw,
    initialStoreState = initialGameState,
    stateReducer = eventReducer,
    interceptor = stateLoggingInterceptor
)

suspend fun main(): Unit = coroutineScope {
    val stateMachine = tbialStateMachineProvider(this)
    val firstPlayer = stateMachine.store.state.value.currentPlayer

    launch { stateMachine.store.state.collect() }

    stateMachine.send(Event.DrawCards(origin = firstPlayer)).let(::println)
    stateMachine.send(Event.NextTurn(origin = firstPlayer)).let(::println)
    stateMachine.send(Event.DrawCards(origin = firstPlayer)).let(::println)

    stateMachine.send(
        Event.ReactWithCard(
            origin = firstPlayer,
            stateMachine.store.state.value.currentPlayer,
            stateMachine.store.state.value.currentPlayer.cards.random()
        )
    )

    println("Done...")
}
