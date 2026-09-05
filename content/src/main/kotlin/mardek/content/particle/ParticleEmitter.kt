package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import java.io.Serializable
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
	@BitField(id = 0)
	val transform: EmitterTransform,

	/**
	 * The sprite of this particle emitter, which will be rendered at the position of all its emitted particles.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "particle sprites")
	val sprite: ParticleSprite,

	/**
	 * The *waves* of this particle emitter. This determines how often this emitter spawns particles, as well as the
	 * time between each 'wave'.
	 */
	@BitField(id = 2)
	val waves: EmissionWaves,

	/**
	 * The initial position, rotation, and velocity of newly spawned particles
	 */
	@BitField(id = 3)
	val spawn: ParticleSpawnProperties,

	/**
	 * The dynamics (e.g. acceleration and radial acceleration) of the particles
	 */
	@BitField(id = 4)
	val dynamics: ParticleDynamics,

	/**
	 * The sizes of the particles, and their growth/shrink behavior
	 */
	@BitField(id = 5)
	val size: ParticleSize,

	/**
	 * The initial velocity of the particles, and their fade behavior
	 */
	@BitField(id = 6)
	val opacity: ParticleOpacity,

	/**
	 * The (maximum) lifetime of each emitted particle
	 */
	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	val lifeTime: Duration,

	/**
	 * Whether the emitted particle should be mirrored (in the X direction?)
	 */
	@BitField(id = 8)
	val mirror: Boolean,
) : Serializable {
	@Suppress("unused")
	private constructor() : this(
		EmitterTransform(), ParticleSprite(), EmissionWaves(), ParticleSpawnProperties(),
		ParticleDynamics(), ParticleSize(), ParticleOpacity(), Duration.ZERO, false
	)
}
