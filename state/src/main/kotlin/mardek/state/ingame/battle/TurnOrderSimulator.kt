package mardek.state.ingame.battle

import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.state.ingame.characters.CharacterState

class TurnOrderSimulator internal constructor(
	private val entries: List<TurnOrderEntry>
) {

	constructor(
		battleState: BattleState, characterStates: Map<PlayableCharacter, CharacterState>
	) : this((battleState.livingEnemies() + battleState.livingPlayers()).map { TurnOrderEntry(
		combatant = it,
		agility = it.getStat(CombatStat.Agility, characterStates),
		turnsSpent = it.getState().spentTurnsThisRound,
		turnsPerRound = 1 + it.getState().statusEffects.sumOf { effect -> effect.extraTurns },
	) })

	fun checkReset(): Boolean {
		return if (entries.none { it.turnsSpent < it.turnsPerRound }) {
			for (entry in entries) entry.turnsSpent = 0
			true
		} else false
	}

	fun next(): CombatantReference? {
		if (entries.isEmpty()) return null
		val nextPriority = entries.mapNotNull { it.computePriority() }.max()
		val nextEntry = entries.find { it.computePriority() == nextPriority }!!
		nextEntry.turnsSpent += 1
		return nextEntry.combatant
	}
}

internal class TurnOrderEntry(
	val combatant: CombatantReference,
	val agility: Int,
	var turnsSpent: Int,
	val turnsPerRound: Int,
) {
	fun computePriority(): Int? {
		return if (turnsSpent >= turnsPerRound) null
		else agility * (turnsPerRound - turnsSpent) / turnsPerRound
	}
}
