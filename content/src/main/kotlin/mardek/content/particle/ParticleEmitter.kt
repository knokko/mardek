package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.ReferenceField
import kotlin.time.Duration

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
	val transform: EmitterTransform,

	@ReferenceField(stable = false, label = "particle sprites")
	val sprite: ParticleSprite,
	val waves: EmissionWaves,
	val spawn: ParticleSpawnProperties,
	val dynamics: ParticleDynamics,
	val size: ParticleSize,
	val opacity: ParticleOpacity,

	/**
	 * The (maximum) lifetime of each emitted particle
	 */
	val lifeTime: Duration,

	/**
	 * Whether the emitted particle should be mirrored (in the X direction?)
	 */
	val mirror: Boolean,

	// TODO OEFmod:
	// - mod (accelerationXY)
	// - vel (radial.min/maxSpawnVelocity and linear.min/maxSpawnVelocityXY)
) {
}
