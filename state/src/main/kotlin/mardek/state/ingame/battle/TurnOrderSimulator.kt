package mardek.state.ingame.battle

import mardek.content.stats.CombatStat

/**
 * This class is used to compute and predict the turn order of the combatants (during a battle). To use this class:
 * 1. Create an instance: `val simulator = TurnOrderSimulator(battleState, context)`
 * 2. Reset [CombatantState.spentTurnsThisRound] if needed:
 * `if (simulator.checkReset()) components.forEach { it.spentTurnsThisRound = 0 }`
 * If you only want to *predict* the turn order, you should *not* reset all the `spentTurnsThisRound`, but you still
 * need to call `simulator.checkReset()`.
 * 3. Call `simulator.next()` to determine the next combatant that should take a turn.
 * If you call `next()` multiple times, each invocation will determine the combatant that should take a turn *after*
 * the combatant returned by the previous call to `next()`. (This works similarly to an iterator.)
 */
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

	/**
	 * This method must be called at least once before calling [next]. This method will return `true` if all
	 * combatants have already taken all their turns for this round.
	 */
	fun checkReset(): Boolean {
		return if (entries.none { it.turnsSpent < it.turnsPerRound }) {
			for (entry in entries) entry.turnsSpent = 0
			true
		} else false
	}

	/**
	 * Determines the next combatant that should take a turn.
	 *
	 * If you call it more than once, it returns the combatant that should take a turn *after* the combatant
	 * returned by the *previous* invocation of this method. This can be used to predict the entire turn order.
	 *
	 * Note that you must call [checkReset] at least once before calling this method.
	 */
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
