package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = true)
class ParticleSpawnProperties(

	/**
	 * The base X-coordinate of the spawn position of each particle, relative to `transform`
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 1.0)
	val baseX: Float,

	/**
	 * The base Y-coordinate of the spawn position of each particle, relative to `transform`
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0)
	val baseY: Float,

	/**
	 * The X-coordinate of the spawn position of new particles will be increased by `shiftX` every
	 * second (continuously): so the spawn X of new particles after `t` seconds is
	 * `baseX + t * shiftX + (the rest)`.
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 1.0)
	val shiftX: Float,

	/**
	 * The Y-coordinate of the spawn position of new particles will be increased by `shiftY` every
	 * second (continuously): so the spawn Y of new particles after `t` seconds is
	 * `baseY + t * shiftY + (the rest)`.
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 1.0)
	val shiftY: Float,

	/**
	 * A random number between `-variationX / 2` and `variationX / 2` will be added to the
	 * X-coordinate of the spawn position of each particle.
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.1)
	val variationX: Float,

	/**
	 * A random number between `-variationY / 2` and `variationY / 2` will be added to the
	 * Y-coordinate of the spawn position of each particle.
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.1)
	val variationY: Float,

	/**
	 * The value of `variationX` will basically be increased by `shiftVariationX` every second
	 * (continuously): the value of `variationX` after `t` seconds will be computed as
	 * `originalVariationX + t * shiftVariationX`
	 */
	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.1)
	val shiftVariationX: Float,

	/**
	 * The value of `variationY` will basically be increased by `shiftVariationY` every second
	 * (continuously): the value of `variationY` after `t` seconds will be computed as
	 * `originalVariationY + t * shiftVariationY`
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.1)
	val shiftVariationY: Float,

	/**
	 * When `rotation` is `null`, the initial rotation of each particle is random. Otherwise, the
	 * initial rotation of each particle is `rotation`. However, this property is ignored when
	 * this emitter is radial and `rotateToMoveDirection` is true.
	 */
	@BitField(id = 8, optional = true)
	@FloatField(expectMultipleOf = 1.0)
	val rotation: Float?,

	/**
	 * The `rotation` of new particles is multiplied by `rotationMultiplier` every second (continuously)
	 */
	@BitField(id = 9)
	@FloatField(expectMultipleOf = 1.0)
	val rotationMultiplier: Float,

	@BitField(id = 10, optional = true)
	val linear: LinearParticleSpawnProperties?,

	@BitField(id = 11, optional = true)
	val radial: RadialParticleSpawnProperties?,
) {
	internal constructor() : this(
		0f, 0f, 0f, 0f,
		0f, 0f, 0f, 0f,
		null, 0f, null, null
	)
}
