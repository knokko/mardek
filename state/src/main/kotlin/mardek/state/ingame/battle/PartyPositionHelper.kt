package mardek.state.ingame.battle

import mardek.content.battle.PartyLayout
import mardek.content.battle.PartyLayoutPosition
import mardek.input.InputKey
import kotlin.math.abs

fun closestTarget(
	target: PartyLayoutPosition, states: Array<CombatantState?>, positions: PartyLayout
) = states.withIndex().filter { it.value != null }.minBy { abs(positions.positions[it.index].y - target.y) }.index

private fun nextDown(current: Int, states: Array<CombatantState?>, positions: PartyLayout): Int {
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index] != null && it.value.y > positions.positions[current].y
	}.minByOrNull { it.value.y }
	return nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index] != null
	}.minBy { it.value.y }.index
}

private fun nextUp(current: Int, states: Array<CombatantState?>, positions: PartyLayout): Int {
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index] != null && it.value.y < positions.positions[current].y
	}.maxByOrNull { it.value.y }
	return nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index] != null
	}.maxBy { it.value.y }.index
}

fun nextTarget(
	key: InputKey, currentTarget: CombatantReference, state: BattleState,
): CombatantReference {
	val nextIndex = if (currentTarget.isPlayer) {
		if (key == InputKey.MoveDown) {
			nextDown(currentTarget.index, state.playerStates, state.playerLayout)
		} else {
			nextUp(currentTarget.index, state.playerStates, state.playerLayout)
		}
	} else {
		if (key == InputKey.MoveDown) {
			nextDown(currentTarget.index, state.enemyStates, state.battle.enemyLayout)
		} else {
			nextUp(currentTarget.index, state.enemyStates, state.battle.enemyLayout)
		}
	}
	return CombatantReference(currentTarget.isPlayer, nextIndex, state)
}
