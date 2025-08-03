package mardek.state.ingame.battle

import mardek.content.stats.CombatStat

class TurnOrderSimulator internal constructor(
	private val entries: List<TurnOrderEntry>
) {

	constructor(
		battleState: BattleState, context: BattleUpdateContext
	) : this((battleState.livingOpponents() + battleState.livingPlayers()).map { TurnOrderEntry(
		combatant = it,
		agility = it.getStat(CombatStat.Agility, context),
		turnsSpent = it.spentTurnsThisRound,
		turnsPerRound = 1 + it.statusEffects.sumOf { effect -> effect.extraTurns },
	) })

	fun checkReset(): Boolean {
		return if (entries.none { it.turnsSpent < it.turnsPerRound }) {
			for (entry in entries) entry.turnsSpent = 0
			true
		} else false
	}

	fun next(): CombatantState? {
		if (entries.isEmpty()) return null
		val nextPriority = entries.mapNotNull { it.computePriority() }.max()
		val nextEntry = entries.find { it.computePriority() == nextPriority }!!
		nextEntry.turnsSpent += 1
		return nextEntry.combatant
	}
}

internal class TurnOrderEntry(
	val combatant: CombatantState,
	val agility: Int,
	var turnsSpent: Int,
	val turnsPerRound: Int,
) {
	fun computePriority(): Int? {
		return if (turnsSpent >= turnsPerRound) null
		else agility * (turnsPerRound - turnsSpent) / turnsPerRound
	}
}
