package mardek.content.particle

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = true)
class LinearParticleSpawnProperties(
	/**
	 * The minimum initial X velocity of the emitted particles
	 */
	val minVelocityX: Float,

	/**
	 * The maximum initial X velocity of the emitted particles
	 */
	val maxVelocityX: Float,

	/**
	 * The minimum initial Y velocity of the emitted particles
	 */
	val minVelocityY: Float,

	/**
	 * The maximum initial Y velocity of the emitted particles
	 */
	val maxVelocityY: Float,
) {
}
