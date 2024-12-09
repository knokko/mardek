package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class GemProperties(
		@BitField(ordering = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val power: Int,

		// TODO third effect

		@BitField(ordering = 0)
		@FloatField(expectMultipleOf = 0.25)
		val drainHp: Float,
) {

	@Suppress("unused")
	private constructor() : this(0, 0f)

	override fun toString() = "Gem($power, $drainHp)"
}
