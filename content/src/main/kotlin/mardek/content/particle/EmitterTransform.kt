package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

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
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 1.0)
	val x: Float,

	/**
	 * - 0 ~= centre of target model
	 * - negative is up
	 * - positive is down
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0)
	val y: Float,

	/**
	 * The rotation, in degrees
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 1.0)
	val rotation: Float,
) {
	internal constructor() : this(0f, 0f, 0f)
}
