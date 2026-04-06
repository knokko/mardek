package mardek.state.ingame.battle

import mardek.content.battle.PartyLayout
import mardek.content.battle.PartyLayoutPosition
import mardek.input.InputKey
import kotlin.math.abs

/**
 * Finds the position in [positions] of the living combatant whose position is closest to [target]
 */
fun closestTarget(
	target: PartyLayoutPosition, states: Array<CombatantState?>, positions: PartyLayout
) = states.withIndex().filter {
	it.value?.isAlive() == true
}.minBy {
	abs(positions.positions[it.index].distanceY - target.distanceY)
}.index

private fun nextDown(current: CombatantState, states: Array<CombatantState?>, positions: PartyLayout): CombatantState {
	val currentIndex = states.indexOf(current)
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true && it.value.distanceY > positions.positions[currentIndex].distanceY
	}.minByOrNull { it.value.distanceY }
	return states[nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true
	}.minBy { it.value.distanceY }.index]!!
}

private fun nextUp(current: CombatantState, states: Array<CombatantState?>, positions: PartyLayout): CombatantState {
	val currentIndex = states.indexOf(current)
	val nextIndex = positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true && it.value.distanceY < positions.positions[currentIndex].distanceY
	}.maxByOrNull { it.value.distanceY }
	return states[nextIndex?.index ?: positions.positions.withIndex().filter {
		states[it.index]?.isAlive() == true
	}.maxBy { it.value.distanceY }.index]!!
}

/**
 * This method is used in the vertical scrolling of the target selection logic:
 * - When `key == InputKey.MoveDown`, this finds the next living combatant *below* [currentTarget]. When [currentTarget]
 * is already the 'lowest' living combatant on its team, the 'highest' living combatant on its team is returned instead.
 * - When `key == InputKey.MoveUp`, this finds the next living combatant *above* [currentTarget]. When [currentTarget]
 * is already the 'highest' living combatant on its team, the 'lowest' living combatant on its team is returned instead.
 */
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
