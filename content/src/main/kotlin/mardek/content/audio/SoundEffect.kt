package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID

@BitStruct(backwardCompatible = true)
class SoundEffect(
	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", writeAsBytes = true)
	val oggData: ByteArray
) {

	@BitField(id = 2)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", ByteArray(0))

	override fun toString() = flashName
}
