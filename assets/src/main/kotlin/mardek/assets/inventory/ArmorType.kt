package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class ArmorType(
	@BitField(ordering = 0)
	val key: String,

	@BitField(ordering = 1)
	val name: String,
) {

	internal constructor() : this("", "")

	override fun toString() = "$name ($key)"
}
