package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class GemProperties(
		@BitField(ordering = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val power: Int,

		@BitField(ordering = 1)
		val rawName: String, // TODO Figure out what this means
) {

	@Suppress("unused")
	private constructor() : this(0, "")

	override fun toString() = "Gem($power, $rawName)"
}
