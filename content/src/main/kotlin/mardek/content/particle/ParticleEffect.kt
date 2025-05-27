package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class ParticleEffect(

	val name: String,
	/**
	 * The additional delay between the start of the particle effect, and the time at which the target takes damage
	 * (or gets healed/cured)
	 */
	val damageDelay: Duration,

	/**
	 * The sound played when the particle effect begins
	 */
	@ReferenceField(stable = false, label = "sound effects")
	val initialSound: SoundEffect,

	/**
	 * The sound played when the target takes damage (or gets healed or cured)
	 */
	val damageSound: SoundEffect?,

	/**
	 * Only used for earthquake
	 */
	val quake: ParticleQuake?,

	/**
	 * Only used for razor leaf (it's an empty array for all other skills): `initialSound` should be played again
	 * after each duration in `extraSoundDelays` (since the start of the particle effect)
	 */
	val extraSoundDelays: Array<Duration>,

	val emitters: ArrayList<ParticleEmitter>,
) {
}
