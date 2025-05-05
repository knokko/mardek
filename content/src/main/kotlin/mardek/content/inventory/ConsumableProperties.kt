package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.StatModifierRange

@BitStruct(backwardCompatible = true)
class ConsumableProperties(
		@BitField(id = 0, optional = true)
		val particleEffect: String?, // TODO Make reference?

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		val particleColor: Int,

		@BitField(id = 2)
		val isFullCure: Boolean,

		@BitField(id = 3)
		@IntegerField(expectUniform = false, minValue = 0)
		val restoreHealth: Int,

		@BitField(id = 4)
		@IntegerField(expectUniform = false, minValue = 0)
		val restoreMana: Int,

		@BitField(id = 5)
		@FloatField(expectMultipleOf = 0.5)
		val revive: Float,

		@BitField(id = 6)
		val addStatusEffects: ArrayList<PossibleStatusEffect>,

		@BitField(id = 7)
		val removeStatusEffects: ArrayList<PossibleStatusEffect>,

		@BitField(id = 8)
		val removeNegativeStatusEffects: Boolean,

		@BitField(id = 9)
		val statModifiers: ArrayList<StatModifierRange>,

		@BitField(id = 10, optional = true)
		val damage: ConsumableDamage?,
) {

	@Suppress("unused")
	private constructor() : this(
			"", 0, false, 0, 0, 0f, ArrayList(0),
			ArrayList(0), false, ArrayList(0), null
	)

	fun isPositive() = damage == null && addStatusEffects.all { it.effect.isPositive }
}
