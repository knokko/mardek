package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class ItemType(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1)
	val canStack: Boolean
) {

	internal constructor() : this("", false)

	override fun toString() = flashName
}
