package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect

@BitStruct(backwardCompatible = true)
class EmissionWaves(

	/**
	 * The delay (in seconds) before this emitter starts emitting particles
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 1.0 / 30.0)
	val delay: Float,

	/**
	 * The sound effect to be played after the `delay` has passed (so when particles start being emitted)
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val delayedSound: SoundEffect?,

	/**
	 * The time (in seconds) between spawning 2 (rounds of) particles
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 1.0 / 30.0)
	val period: Float,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 1)
	val particlesPerWave: Int,

	/**
	 * The number of rounds/periods
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val numRounds: Int,
) {

	init {
		if (particlesPerWave < 1) {
			throw IllegalArgumentException("At least 1 particle per wave is required, but got $particlesPerWave")
		}
	}

	internal constructor() : this(0f, null, 0f, 1, 1)
}
