package mardek.state.ingame.battle

import mardek.content.battle.PartyLayout
import mardek.content.battle.PartyLayoutPosition
import mardek.input.InputKey
import kotlin.math.abs

fun closestTarget(
	target: PartyLayoutPosition, states: Array<CombatantState?>, positions: PartyLayout
) = states.withIndex().filter { it.value != null }.minBy { abs(positions.positions[it.index].y - target.y) }.index

private fun nextDown(current: CombatantState, states: Array<CombatantState?>, positions: PartyLayout): CombatantState {
	val currentIndex = states.indexOf(current)
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true && it.value.y > positions.positions[currentIndex].y
	}.minByOrNull { it.value.y }
	return states[nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index] != null
	}.minBy { it.value.y }.index]!!
}

private fun nextUp(current: CombatantState, states: Array<CombatantState?>, positions: PartyLayout): CombatantState {
	val currentIndex = states.indexOf(current)
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true && it.value.y < positions.positions[currentIndex].y
	}.maxByOrNull { it.value.y }
	return states[nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index] != null
	}.maxBy { it.value.y }.index]!!
}

fun nextTarget(
	key: InputKey, currentTarget: CombatantState, state: BattleState,
) = if (currentTarget.isOnPlayerSide) {
	if (key == InputKey.MoveDown) {
		nextDown(currentTarget, state.players, state.playerLayout)
	} else {
		nextUp(currentTarget, state.players, state.playerLayout)
	}
} else {
	if (key == InputKey.MoveDown) {
		nextDown(currentTarget, state.opponents, state.battle.enemyLayout)
	} else {
		nextUp(currentTarget, state.opponents, state.battle.enemyLayout)
	}
}
