package mardek.content.particle

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = true)
class ParticleSpawnProperties(
	/**
	 * The base X-coordinate of the spawn position of each particle, relative to `transform`
	 */
	val baseX: Float,

	/**
	 * The base Y-coordinate of the spawn position of each particle, relative to `transform`
	 */
	val baseY: Float,

	/**
	 * The X-coordinate of the spawn position of new particles will be increased by `shiftX` every
	 * second (continuously): so the spawn X of new particles after `t` seconds is
	 * `baseX + t * shiftX + (the rest)`.
	 */
	val shiftX: Float,

	/**
	 * The Y-coordinate of the spawn position of new particles will be increased by `shiftY` every
	 * second (continuously): so the spawn Y of new particles after `t` seconds is
	 * `baseY + t * shiftY + (the rest)`.
	 */
	val shiftY: Float,

	/**
	 * A random number between `-variationX / 2` and `variationX / 2` will be added to the
	 * X-coordinate of the spawn position of each particle.
	 */
	val variationX: Float,

	/**
	 * A random number between `-variationY / 2` and `variationY / 2` will be added to the
	 * Y-coordinate of the spawn position of each particle.
	 */
	val variationY: Float,

	/**
	 * The value of `variationX` will basically be increased by `shiftVariationX` every second
	 * (continuously): the value of `variationX` after `t` seconds will be computed as
	 * `originalVariationX + t * shiftVariationX`
	 */
	val shiftVariationX: Float,

	/**
	 * The value of `variationY` will basically be increased by `shiftVariationY` every second
	 * (continuously): the value of `variationY` after `t` seconds will be computed as
	 * `originalVariationY + t * shiftVariationY`
	 */
	val shiftVariationY: Float,

	/**
	 * When `rotation` is `null`, the initial rotation of each particle is random. Otherwise, the
	 * initial rotation of each particle is `rotation`. However, this property is ignored when
	 * this emitter is radial and `rotateToMoveDirection` is true.
	 */
	val rotation: Float?,

	val linear: LinearParticleSpawnProperties?,
	val radial: RadialParticleSpawnProperties?,
) {
}
