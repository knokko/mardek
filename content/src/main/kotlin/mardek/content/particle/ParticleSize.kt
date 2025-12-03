package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class ParticleSize(
	/**
	 * The 'base' initial width of the emitted particles, which will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier` to determine the initial particle width.
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 1.0)
	val baseWidth: Float,

	/**
	 * The 'base' initial height of the emitted particles, which will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier` (the same random number that was multiplied by `baseWidth`)
	 * to determine the initial particle height.
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 1.0)
	val baseHeight: Float,

	/**
	 * The `baseWidth` for new particles is increased by `shiftWidth` every second (continuously)
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val shiftWidth: Float,

	/**
	 * The `baseHeight` for new particles is increased by `shiftHeight` every second (continuously)
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.1)
	val shiftHeight: Float,

	/**
	 * Whenever a particle is spawned, its width and height will be multiplied by a random number between
	 * `minSizeMultiplier` and `maxSizeMultiplier`.
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.1)
	val minSizeMultiplier: Float,

	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.1)
	val maxSizeMultiplier: Float,

	/**
	 * The width of each particle will be multiplied by `growX` every second (continuously)
	 */
	@BitField(id = 6)
	@FloatField(
		expectMultipleOf = 0.001, commonValues = [1.0],
		expectedIntegerMultiple = IntegerField(expectUniform = false, digitSize = 3),
	)
	val growX: Float,

	/**
	 * The height of each particle will be multiplied by `growY` every second (continuously)
	 */
	@BitField(id = 7)
	@FloatField(
		expectMultipleOf = 0.001, commonValues = [1.0],
		expectedIntegerMultiple = IntegerField(expectUniform = false, digitSize = 3),
	)
	val growY: Float,
) {
	internal constructor() : this(
		0f, 0f, 0f, 0f,
		0f, 0f, 0f, 0f
	)
}
