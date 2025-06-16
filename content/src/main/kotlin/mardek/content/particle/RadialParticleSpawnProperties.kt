package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = true)
class RadialParticleSpawnProperties(

	/**
	 * The minimum distance between the particle and the emitter when the particle is spawned.
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 1.0)
	val minRadius: Float,

	/**
	 * The `minRadius` for new particles is increased by `shiftMinRadius` every second (continuously)
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0)
	val shiftMinRadius: Float,

	/**
	 * The minimum distance between the particle and the emitter when the particle is spawned.
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 1.0)
	val maxRadius: Float,

	/**
	 * The `maxRadius` for new particles is increased by `shiftMaxRadius` every second (continuously)
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 1.0)
	val shiftMaxRadius: Float,

	/**
	 * When the number of spawned particles is larger than 1, this property determines whether particles should be
	 * evenly spaced/divided over 360 degrees when they are spawned. For instance, for 3 particles, they would be
	 * spaced 120 degrees apart.
	 *
	 * When `evenlySpaced == null`, particles are **not** evenly spaced. When `evenlySpaced != null`, particles are
	 * evenly spaced, and the direction of the first particle will be `evenlySpaced` degrees.
	 */
	@BitField(id = 4, optional = true)
	@FloatField(expectMultipleOf = 1.0)
	val evenlySpaced: Float?,

	/**
	 * The value of `evenlySpaced` for new particles (if not null) is increased by `baseEvenAngleShift` every second
	 * (continuously)
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 1.0)
	val shiftEvenlySpaced: Float,

	/**
	 * Whether the particle should initially be rotated towards the direction in which it starts moving
	 */
	@BitField(id = 6)
	val rotateToMoveDirection: Boolean,

	/**
	 * The minimum initial velocity (units / second), towards the spawn direction of the particle
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 1.0)
	val minVelocity: Float,

	/**
	 * The maximum initial velocity (units / second), towards the spawn direction of the particle
	 */
	@BitField(id = 8)
	@FloatField(expectMultipleOf = 1.0)
	val maxVelocity: Float,
) {
	@Suppress("unused")
	private constructor() : this(
		0f, 0f, 0f, 0f, null,
		0f, false, 0f, 0f
	)
}
