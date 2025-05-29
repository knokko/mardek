package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class ParticleEffect(

	@BitField(id = 0)
	val name: String,

	/**
	 * The additional delay (in seconds) between the start of the particle effect, and the time at which the target
	 * takes damage (or gets healed/cured)
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0 / 30.0)
	val damageDelay: Float,

	/**
	 * The sound played when the particle effect begins
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val initialSound: SoundEffect?,

	/**
	 * The sound played when the target takes damage (or gets healed or cured)
	 */
	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val damageSound: SoundEffect?,

	/**
	 * Only used for earthquake
	 */
	@BitField(id = 4, optional = true)
	val quake: ParticleQuake?,

	/**
	 * Only used for razor leaf (it's an empty array for all other skills): `initialSound` should be played again
	 * after each duration (in seconds) in `extraSoundDelays` (since the start of the particle effect)
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 1.0 / 30.0)
	val extraSoundDelays: FloatArray,

	@BitField(id = 6, optional = true)
	val inheritance: ParticleInheritance?,

	@BitField(id = 7)
	val emitters: ArrayList<ParticleEmitter>,
) {
	internal constructor() : this(
		"", 0f, null, null, null,
		FloatArray(0), null, ArrayList(0)
	)
}
