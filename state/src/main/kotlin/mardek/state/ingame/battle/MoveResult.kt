package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import java.util.EnumMap

class MoveResult(
	/**
	 * The element icon that should be displayed in the damage indicator **of the caster/attacker**
	 */
	val element: Element,

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

	@BitStruct(backwardCompatible = true)
	class Entry(
		/**
		 * The element icon that should be displayed in the damage indicator **of the target**
		 */
		@BitField(id = 0)
		@ReferenceField(stable = true, label = "elements")
		val element: Element,

		/**
		 * When non-zero, overrides the 'blink color' of the target if it takes damage
		 */
		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		val overrideBlinkColor: Int,

		/**
		 * The target described by this entry
		 */
		@BitField(id = 2)
		@ReferenceField(stable = false, label = "combatants")
		val target: CombatantState,

		/**
		 * The final amount of damage dealt.
		 * When negative, the move heals the target instead
		 */
		@BitField(id = 3)
		@IntegerField(expectUniform = false)
		val damage: Int,

		/**
		 * The final amount of mana that this target will lose.
		 * When negative, the move restores mana of the target instead.
		 */
		@BitField(id = 4)
		@IntegerField(expectUniform = false)
		val damageMana: Int,

		/**
		 * True if the attack missed, false otherwise.
		 * When true, all other fields should be ignored.
		 */
		@BitField(id = 5)
		var missed: Boolean,

		/**
		 * Whether the attack was a critical hit.
		 * Note that the `damage` is already increased to account for this, so it should not be increased again.
		 */
		@BitField(id = 6)
		var criticalHit: Boolean,

		/**
		 * The status effects that should be removed from the target. This set will only contain status effects that the
		 * target currently has, and never contain auto-effects of the target.
		 */
		@BitField(id = 7)
		@ReferenceField(stable = true, label = "status effects")
		val removedEffects: HashSet<StatusEffect>,

		/**
		 * The status effects that should be added to the target (resistances were already taken into account).
		 * Note that these effects should be added **after** the effects in `removedEffects` are removed.
		 */
		@BitField(id = 8)
		@ReferenceField(stable = true, label = "status effects")
		val addedEffects: HashSet<StatusEffect>,

		/**
		 * The stat modifiers that should be added to the target
		 */
		@BitField(id = 9)
		@NestedFieldSetting(path = "v", fieldName = "ADDED_STAT_MODIFIER_VALUES")
		val addedStatModifiers: EnumMap<CombatStat, Int>,
	) {

		@Suppress("unused")
		private constructor() : this(
			Element(), 0, MonsterCombatantState(), 0, 0, false,
			false, HashSet(), HashSet(),
			EnumMap(CombatStat::class.java),
		)

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

		companion object {

			@Suppress("unused")
			@IntegerField(expectUniform = false)
			private const val ADDED_STAT_MODIFIER_VALUES = false
		}
	}
}
