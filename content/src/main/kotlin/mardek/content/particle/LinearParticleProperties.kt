package mardek.content.particle

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = true)
class LinearParticleProperties(

	/**
	 * Initial rotation *of the emitter*, in degrees
	 */
	val spawnRotation: Float,

	/**
	 * The rotation *of the emitter* after `t` seconds is `spawnRotation * pow(rotationMultiplier, t)`
	 */
	val rotationMultiplier: Float,
	// TODO Hm... figure out the difference between offset[2] and rot

	/**
	 * A random number between `0` and `maxExtraSpawnOffsetX` will be added to the X-coordinate of the spawn position
	 * of each particle.
	 */
	val maxExtraSpawnOffsetX: Float,

	/**
	 * A random number between `0` and `maxExtraSpawnOffsetY` will be added to the Y-coordinate of the spawn position
	 * of each particle.
	 */
	val maxExtraSpawnOffsetY: Float,

	/**
	 * The minimum initial X velocity of the emitted particles
	 */
	val minSpawnVelocityX: Float,

	/**
	 * The maximum initial X velocity of the emitted particles
	 */
	val maxSpawnVelocityX: Float,

	/**
	 * The minimum initial Y velocity of the emitted particles
	 */
	val minSpawnVelocityY: Float,

	/**
	 * The maximum initial Y velocity of the emitted particles
	 */
	val maxSpawnVelocityY: Float,
) {
}
