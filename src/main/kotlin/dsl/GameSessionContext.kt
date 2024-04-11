package dsl

import common.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface GameSessionContext {
    val gameState: StateFlow<GameState>
}

interface ServerGameSessionContext {
    val gameState: MutableStateFlow<GameState>
}