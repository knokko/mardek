package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class WeaponType(
	@BitField(ordering = 0, optional = true)
	val flashName: String?
) {

	internal constructor() : this(null)
}
