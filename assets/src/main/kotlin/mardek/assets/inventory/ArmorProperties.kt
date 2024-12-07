package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class ArmorProperties(
		@BitField(ordering = 0)
		@ReferenceField(stable = false, label = "armor types")
		val type: ArmorType,
) {

	@Suppress("unused")
	private constructor() : this(ArmorType())

	override fun toString() = "$type"
}
