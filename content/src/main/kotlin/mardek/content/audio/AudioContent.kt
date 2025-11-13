package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * The audio-related part of the `Content`
 */
@BitStruct(backwardCompatible = true)
class AudioContent {

	/**
	 * This is the list of sound effects that can be used by e.g. skills, but are not needed by the hardcoded actions
	 * like UI.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "sound effects")
	val effects = ArrayList<SoundEffect>()

	/**
	 * This contains the sound effects that are needed by hardcoded actions, for instance the click sounds and the
	 * chest sounds.
	 */
	@BitField(id = 1)
	lateinit var fixedEffects: FixedSoundEffects
}
