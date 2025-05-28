package mardek.content.particle

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = true)
class EmitterTransform(

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
	val x: Float,

	/**
	 * - 0 ~= centre of target model
	 * - negative is up
	 * - positive is down
	 */
	val y: Float,

	/**
	 * The initial rotation, in degrees
	 */
	val initialRotation: Float,

	/**
	 * The rotation of the emitter is multiplied by `rotationMultiplier` every second (continuously).
	 * So, after `t` seconds, the rotation of the emitter will be `initialRotation * pow(rotationMultiplier, t)`
	 */
	val rotationMultiplier: Float,
) {
}
