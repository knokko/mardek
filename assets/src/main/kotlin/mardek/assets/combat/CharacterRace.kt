package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class CharacterRace(
	@BitField(ordering = 0, optional = true)
	val flashName: String?
) {

	internal constructor() : this(null)
}
