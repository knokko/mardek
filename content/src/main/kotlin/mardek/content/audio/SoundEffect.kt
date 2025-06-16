package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = true)
class SoundEffect(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", writeAsBytes = true)
	val oggData: ByteArray
) {

	constructor() : this("", ByteArray(0))

	override fun toString() = flashName
}
