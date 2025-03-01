package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class WeaponType(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1, optional = true)
	val soundEffect: String?,
) {

	constructor() : this("", null)

	override fun toString() = flashName
}
