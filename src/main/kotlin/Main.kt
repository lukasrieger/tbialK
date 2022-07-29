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
import state.stateStore

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
    State.Stumbling into State.PlayCards via Event.DrawCards
    State.PlayCards into State.Cede via Event.NextTurn
}

val tbialStateMachineProvider = { scope: CoroutineScope ->
    tbialStateMachineConfig(
        initialState = State.Init,
        initialStoreState = initialGameState,
        stateReducer = eventReducer,
        scope = scope
    )
}

suspend fun main(): Unit = coroutineScope {
    val stateMachine = tbialStateMachineProvider(this)
    val store = stateStore(initialGameState, eventReducer, stateLoggingInterceptor)

    launch { store.state.collect() }

    store.send(Event.DrawCards)
    store.send(Event.NextTurn)

    store.send(Event.DrawCards)
    store.send(Event.ReactWithCard(store.state.value.currentPlayer, store.state.value.currentPlayer.cards.first()))
}
