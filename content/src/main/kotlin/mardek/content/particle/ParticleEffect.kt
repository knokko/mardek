package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import mardek.content.util.DurationArray
import java.io.Serializable
import kotlin.time.Duration

/**
 * Represents a particle effect. Every particle effect has 0 or more emitters, as well as 0 or more sounds.
 * Every active skill has at most 1 particle effect.
 */
@BitStruct(backwardCompatible = true)
class ParticleEffect(

	/**
	 * The name of the particle effect, which is only used for debugging and importing
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The additional delay between the start of the particle effect, and the time at which the target
	 * takes damage (or gets healed/cured)
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val damageDelay: Duration,

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
	 * after each duration in `extraSoundDelays` (since the start of the particle effect)
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	val extraSoundDelays: DurationArray,

	/**
	 * Some particle effects *inherit* their properties from another particle effect, but *override* 1 or more
	 * properties. For instance, all gemsplosion variants inherit their properties from one 'parent' particle effect,
	 * and override the texture. This avoids the need to repeat everything else (which is the same).
	 *
	 * When this field is `null`, the particle effect doesn't inherit anything from other particle effects.
	 */
	@BitField(id = 6, optional = true)
	val inheritance: ParticleInheritance?,

	/**
	 * The particle emitters of this particle effect. Each emitter spawns one type of particle using the dynamics of
	 * the emitter.
	 */
	@BitField(id = 7)
	val emitters: ArrayList<ParticleEmitter>,
) : Serializable {
	constructor() : this(
		"", Duration.ZERO, null, null, null,
		DurationArray(emptyArray()), null, ArrayList(0)
	)

	override fun toString() = name

	/**
	 * Gets the emitters that this particle effect should use (either its own emitters, or the emitters inherited from
	 * its parent).
	 */
	fun emitters() = inheritance?.parent?.emitters ?: emitters

	/**
	 * Gets the sound that should be played when this particle effect is spawned (either its own sound, or the sound
	 * inherited from its parent)
	 */
	fun initialSound() = initialSound ?: inheritance?.parent?.initialSound

	/**
	 * Gets the sound that should be played when this particle effect deals damage to the target of the skill
	 * (either its own sound, or the sound inherited from its parent)
	 */
	fun damageSound() = damageSound ?: inheritance?.parent?.damageSound

	/**
	 * Gets the time between the start of the particle effect, and the time at which it should deal damage
	 * to the (first) target. This can be either the damage delay of this particle effect, or the damage delay that it
	 * inherits from its parent.
	 */
	fun damageDelay() = inheritance?.parent?.damageDelay ?: damageDelay
}
