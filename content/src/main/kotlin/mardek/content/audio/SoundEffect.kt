package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID

/**
 * Represents a sound effect. This is just the data of an ogg file, along with a name and ID.
 */
@BitStruct(backwardCompatible = true)
class SoundEffect(

	/**
	 * The name of the sound effect, as imported from Flash. This is not used in-game, but potentially useful for
	 * debugging and editing.
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The sound data: this is the content of the corresponding .ogg file.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "", writeAsBytes = true)
	val oggData: ByteArray,

	/**
	 * The unique ID of the sound effect, which is used for (de)serialization.
	 */
	@BitField(id = 2)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this("", ByteArray(0), UUID.randomUUID())

	override fun toString() = flashName
}
