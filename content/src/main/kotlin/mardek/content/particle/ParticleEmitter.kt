package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.animations.ColorTransform
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
	val waves: EmissionWaves,

	@ReferenceField(stable = false, label = "particle sprites")
	val sprite: ParticleSprite, // derived from "type" and "sheet"

	val colorTransform: ColorTransform,

	val radial: RadialParticleProperties?,
	val linear: LinearParticleProperties?,

	val opacity: ParticleOpacity,

	/**
	 * The (maximum) lifetime of each emitted particle
	 */
	val lifeTime: Duration,

	/**
	 * - 0 ~= centre of target model
	 * Enemies:
	 * - negative is left
	 * - positive is right
	 *
	 * Players:
	 * - negative is right
	 * - positive is left
	 */
	val emitterSpawnX: Float,

	/**
	 * - 0 ~= centre of target model
	 * - negative is up
	 * - positive is down
	 */
	val emitterSpawnY: Float,


	/**
	 * The X-coordinate of the spawn position of each particle, relative to the emitter
	 */
	val particleSpawnX: Float,

	/**
	 * The Y-coordinate of the spawn position of each particle, relative to the emitter
	 */
	val particleSpawnY: Float,

	/**
	 * This value is added to the X velocity of each particle, every second (gradually)
	 * TODO closed form?
	 */
	val accelerationX: Float,

	/**
	 * This flat value is added to the Y velocity for each particle update
	 */
	val accelerationY: Float,

	/**
	 * The X velocity of each emitted particle update will be multiplied by
	 * `pow(accelerationX, time since particle spawn)`
	 */
	val velocityMultiplierX: Float,

	/**
	 * The Y velocity of each emitted particle update will be multiplied by
	 * `pow(accelerationX, time since particle spawn)`
	 */
	val velocityMultiplierY: Float,

	/**
	 * During each emitted particle update, the velocity will be increased by
	 * `initialDirection * pow(radialAcceleration, time since particle spawn)`
	 */
	val radialAcceleration: Float,

	/**
	 * The 'base' initial width of the emitted particles
	 */
	val baseWidth: Float,

	/**
	 * The 'base' initial height of the emitted particles
	 */
	val baseHeight: Float,

	/**
	 * Whenever a particle is spawned, its width and height will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier`.
	 */
	val minSizeMultiplier: Float,
	val maxSizeMultiplier: Float,

	/**
	 * At any point in time, the width of each particle is multiplied by `pow(growX, time since particle spawned)`
	 */
	val growX: Int,

	/**
	 * At any point in time, the height of each particle is multiplied by `pow(growY, time since particle spawned)`
	 */
	val growY: Int,

	/**
	 * When `initialParticleRotation` is `null`, the initial rotation of each particle is random. Otherwise, the
	 * initial rotation of each particle is `initialParticleRotation`. However, this property is ignored when
	 * this emitter is radial and `rotateToMoveDirection` is true.
	 */
	val initialParticleRotation: Float?,

	/**
	 * The particle spin, in degrees per second: during each emitted particle update, the rotation of the particle
	 * will be increased by `deltaTime * spin` degrees.
	 */
	val spin: Float,

	/**
	 * Whether the emitted particle should be mirrored (in the X direction?)
	 */
	val mirror: Boolean,

	// TODO OEFmod:
	// - Coffset (particleSpawnXY)
	// - offset (radial.min/maxspawnRadius)
	// - start_angle (baseEvenAngle)
	// - area (linear.maxExtraSpawnOffsetXY)
	// - size (baseWidth and baseHeight)
	// - mod (accelerationXY)
	// - vel (radial.min/maxSpawnVelocity and linear.min/maxSpawnVelocityXY)
) {
}
