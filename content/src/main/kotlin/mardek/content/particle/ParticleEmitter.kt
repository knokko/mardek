package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField

/**
 * `ParticleEmitter`s spawn/emit one or more particles.
 *
 * Both the emitter and the particles have their own position. Whenever the emitter spawns a particle, the particle is
 * spawned at the position of the emitter, plus some optional offsets.
 *
 * The emitter position is relative to the position of the **particle effect** (usually around the belly of the
 * combatant).
 */
@BitStruct(backwardCompatible = true)
class ParticleEmitter(

	/**
	 * The position/orientation **of the emitter**, which is the **parent transformation** of all its particles.
	 */
	@BitField(id = 0)
	val transform: EmitterTransform,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "particle sprites")
	val sprite: ParticleSprite,

	@BitField(id = 2)
	val waves: EmissionWaves,

	@BitField(id = 3)
	val spawn: ParticleSpawnProperties,

	@BitField(id = 4)
	val dynamics: ParticleDynamics,

	@BitField(id = 5)
	val size: ParticleSize,

	@BitField(id = 6)
	val opacity: ParticleOpacity,

	/**
	 * The (maximum) lifetime of each emitted particle (in seconds)
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 1.0 / 30.0, commonValues=[6.666, 3.333, 1.0, 0.666])
	val lifeTime: Float,

	/**
	 * Whether the emitted particle should be mirrored (in the X direction?)
	 */
	@BitField(id = 8)
	val mirror: Boolean,
) {
	@Suppress("unused")
	private constructor() : this(
		EmitterTransform(), ParticleSprite(), EmissionWaves(), ParticleSpawnProperties(),
		ParticleDynamics(), ParticleSize(), ParticleOpacity(), 0f, false
	)
}
