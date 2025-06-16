package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = true)
class LinearParticleSpawnProperties(
	/**
	 * The minimum initial X velocity of the emitted particles
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.1)
	val minVelocityX: Float,

	/**
	 * The `minVelocityX` of new particles is increased by `shiftMinVelocityX` every second (continuously)
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.1)
	val shiftMinVelocityX: Float,

	/**
	 * The maximum initial X velocity of the emitted particles
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val maxVelocityX: Float,

	/**
	 * The `maxVelocityX` of new particles is increased by `shiftMaxVelocityX` every second (continuously)
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.1)
	val shiftMaxVelocityX: Float,

	/**
	 * The minimum initial Y velocity of the emitted particles
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.1)
	val minVelocityY: Float,

	/**
	 * The `minVelocityY` of new particles is increased by `shiftMinVelocityY` every second (continuously)
	 */
	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.1)
	val shiftMinVelocityY: Float,

	/**
	 * The maximum initial Y velocity of the emitted particles
	 */
	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.1)
	val maxVelocityY: Float,

	/**
	 * The `maxVelocityY` of new particles is increased by `shiftMaxVelocityY` every second (continuously)
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.1)
	val shiftMaxVelocityY: Float,
) {
	@Suppress("unused")
	private constructor() : this(
		0f, 0f, 0f, 0f,
		0f, 0f, 0f, 0f
	)
}
