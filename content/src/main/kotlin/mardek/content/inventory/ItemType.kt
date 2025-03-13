package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class ItemType(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	val canStack: Boolean
) {

	internal constructor() : this("", false)

	override fun toString() = flashName
}
