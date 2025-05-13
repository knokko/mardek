package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class AudioContent {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "sound effects")
	val effects = ArrayList<SoundEffect>()

	@BitField(id = 1)
	lateinit var fixedEffects: FixedSoundEffects
}
