package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class ArmorType(
	@BitField(id = 0)
	val key: String,

	@BitField(id = 1)
	val name: String,

	@BitField(id = 2)
	val slot: EquipmentSlotType,
) {

	constructor() : this("", "", EquipmentSlotType.Body)

	override fun toString() = "$name ($key)"
}
