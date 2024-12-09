package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifierRange

@BitStruct(backwardCompatible = false)
class ConsumableProperties(
		@BitField(ordering = 0, optional = true)
		val particleEffect: String?, // TODO Make reference?

		@BitField(ordering = 1)
		@IntegerField(expectUniform = true)
		val particleColor: Int,

		@BitField(ordering = 2)
		val isFullCure: Boolean,

		@BitField(ordering = 3)
		@IntegerField(expectUniform = false, minValue = 0)
		val restoreHealth: Int,

		@BitField(ordering = 4)
		@IntegerField(expectUniform = false, minValue = 0)
		val restoreMana: Int,

		@BitField(ordering = 5)
		@FloatField(expectMultipleOf = 0.5)
		val revive: Float,

		@BitField(ordering = 6)
		val addStatusEffects: ArrayList<PossibleStatusEffect>,

		@BitField(ordering = 7)
		val removeStatusEffects: ArrayList<PossibleStatusEffect>,

		@BitField(ordering = 8)
		val removeNegativeStatusEffects: Boolean,

		@BitField(ordering = 9)
		val statModifiers: ArrayList<StatModifierRange>,

		@BitField(ordering = 10, optional = true)
		val damage: ConsumableDamage?,
) {

	@Suppress("unused")
	private constructor() : this(
			"", 0, false, 0, 0, 0f, ArrayList(0),
			ArrayList(0), false, ArrayList(0), null
	)
}
