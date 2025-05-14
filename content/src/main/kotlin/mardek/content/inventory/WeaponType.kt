package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect

@BitStruct(backwardCompatible = true)
class WeaponType(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val soundEffect: SoundEffect?,
) {

	constructor() : this("", null)

	override fun toString() = flashName
}
