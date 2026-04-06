package mardek.state.ingame.battle

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Determines the [BattleStateMachine.NextTurnEffects] of the combatant that should take the next turn.
 * - This method will compute all the damage and/or healing that the combatant should get from status effects.
 * - Furthermore, it determines whether the turn should get skipped or forced due to status effects
 * (e.g. Paralysis or Berserk).
 * - Finally, it determines which status effects should get removed at the start of the turn (e.g. Sleep and Confusion).
 */
fun computeStatusEffectsBeforeTurn(
	combatant: CombatantState, context: BattleUpdateContext
): BattleStateMachine.NextTurnEffects {
	val removedEffects = combatant.statusEffects.filter { effect ->
		if (combatant.getAutoEffects(context).contains(effect)) return@filter false
		val removeChance = effect.disappearChancePerTurn + combatant.getResistance(effect, context) / 5
		Random.nextInt(100) < removeChance
	}
	val remainingEffects = combatant.statusEffects - removedEffects.toSet()

	val takeDamage = remainingEffects.filter { it.damagePerTurn != null }
		.sortedByDescending { it.damagePerTurn!!.hpFraction }.map { effect ->
			val dpt = effect.damagePerTurn!!
			var damage = (combatant.maxHealth * dpt.hpFraction).roundToInt()
			if (dpt.hpFraction > 0f) {
				damage = max(damage, 3)
			} else {
				if (combatant is MonsterCombatantState) {
					damage = (20f * dpt.hpFraction * combatant.getLevel(context)).roundToInt()
				}

				if (combatant.revertsHealing()) damage = abs(damage)
			}
			BattleStateMachine.NextTurnEffects.TakeDamage(damage, effect)
		}

	var forceTurn: BattleStateMachine.NextTurnEffects.ForceMove? = null
	for (effect in remainingEffects) {
		val skipTurn = effect.skipTurn
		if (skipTurn != null && Random.Default.nextInt(100) < skipTurn.chance) {
			forceTurn = BattleStateMachine.NextTurnEffects.ForceMove(
				BattleStateMachine.Wait(), effect,
				skipTurn.blinkColor, skipTurn.particleEffect
			)
		}
	}

	val effects = BattleStateMachine.NextTurnEffects(combatant, forceTurn)
	effects.removedEffects.addAll(removedEffects)
	effects.takeDamage.addAll(takeDamage)
	return effects
}
