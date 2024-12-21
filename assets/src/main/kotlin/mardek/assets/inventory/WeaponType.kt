package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class WeaponType(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1, optional = true)
	val soundEffect: String?,
) {

	constructor() : this("", null)

	override fun toString() = flashName
}
