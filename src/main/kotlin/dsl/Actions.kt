package dsl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import common.cards.Card

typealias TurnResult = Either<Unit, List<Card>>

val sequencedAction = program {
    attemptToHandleStumblingCard()
    attackAnotherPlayer()
    finishTurn()
}

val attemptToHandleStumblingCard = program {
    when (checkStumbling()) {
        is Card -> handleStumbling(via = selectResponse())
        else -> Outcome.Success
    }
}

val attackAnotherPlayer = program {
    val targetPlayer = selectPlayer()
    val bugCard = selectAttack()

    attackPlayer(target = targetPlayer, bug = bugCard)
}


val finishTurn = program {
    val currentGameState = getGameState()
    val ownMentalHealth = currentGameState.currentPlayer.mentalHealth
    val currentlyHeldCards = currentGameState.currentPlayer.cards.size

    when {
        currentlyHeldCards > ownMentalHealth ->
            discard(selectDiscardCards(amount = currentlyHeldCards - ownMentalHealth))
        else -> Unit
    }
}
