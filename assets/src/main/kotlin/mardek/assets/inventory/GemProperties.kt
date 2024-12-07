package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.combat.PossibleStatusEffect

@BitStruct(backwardCompatible = false)
class GemProperties(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val power: Int,

	@BitField(ordering = 1)
	val particleEffect: String, // TODO Turn into reference

	@BitField(ordering = 2)
	val inflictStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 3)
	@FloatField(expectMultipleOf = 0.25)
	val drainHp: Float,
) {

	@Suppress("unused")
	private constructor() : this(0, "", ArrayList(0), 0f)

	override fun toString() = "Gem($power, $drainHp)"
}
