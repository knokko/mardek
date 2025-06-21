package mardek.state.ingame.battle

import mardek.content.audio.SoundEffect
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect

class MoveResult(
	/**
	 * The element icon that should be displayed in the damage indicator
	 */
	val element: Element,

	/**
	 * When non-zero, overrides the 'blink color' of the target if it takes damage
	 */
	val overrideBlinkColor: Int,

	/**
	 * The sounds that should be played
	 */
	val sounds: List<SoundEffect>,

	val targets: List<Entry>,

	/**
	 * The amount of health that should be given to the attacker/caster (e.g. drained health). When this is negative,
	 * the attacker should lose health.
	 */
	val restoreAttackerHealth: Int,

	/**
	 * The amount of mana that should be given to the attacker/caster (e.g. Emela wands). When this is negative,
	 * the attacker should lose mana.
	 */
	val restoreAttackerMana: Int,
) {
	override fun toString(): String {
		val result = StringBuilder("MoveResult($element, sounds=$sounds, targets=$targets")
		if (restoreAttackerHealth != 0) {
			result.append(", restore $restoreAttackerHealth hp")
		}
		if (restoreAttackerMana != 0) {
			result.append(", restore $restoreAttackerMana mp")
		}
		result.append(")")
		return result.toString()
	}

	class Entry(
		/**
		 * The target described by this entry
		 */
		val target: CombatantState,

		/**
		 * The final amount of damage dealt.
		 * When negative, the move heals the target instead
		 */
		val damage: Int,

		/**
		 * The final amount of mana that this target will lose.
		 * When negative, the move restores mana of the target instead.
		 */
		val damageMana: Int,

		/**
		 * True if the attack missed, false otherwise.
		 * When true, all other fields should be ignored.
		 */
		var missed: Boolean,

		/**
		 * Whether the attack was a critical hit.
		 * Note that the `damage` is already increased to account for this, so it should not be increased again.
		 */
		var criticalHit: Boolean,

		/**
		 * The status effects that should be removed from the target. This set will only contain status effects that the
		 * target currently has, and never contain auto-effects of the target.
		 */
		val removedEffects: Set<StatusEffect>,

		/**
		 * The status effects that should be added to the target (resistances were already taken into account).
		 * Note that these effects should be added **after** the effects in `removedEffects` are removed.
		 */
		val addedEffects: Set<StatusEffect>,

		/**
		 * The stat modifiers that should be added to the target
		 */
		val addedStatModifiers: Map<CombatStat, Int>,
	) {
		override fun toString(): String {
			if (missed) return "MISSED"
			val result = StringBuilder("Entry(damage=$damage")
			if (criticalHit) result.append(", CRIT")
			if (removedEffects.isNotEmpty()) {
				result.append(", remove $removedEffects")
			}
			if (addedEffects.isNotEmpty()) {
				result.append(", added $addedEffects")
			}
			if (addedStatModifiers.isNotEmpty()) {
				result.append(", added $addedStatModifiers")
			}
			result.append(")")
			return result.toString()
		}
	}
}
