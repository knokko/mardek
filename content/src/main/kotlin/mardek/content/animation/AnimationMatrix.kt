package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

/**
 * Represents a transformation matrix that is used in animations. It contains the same data as a `Matrix3x2f` from
 * JOML, but is structured differently. It is designed to be compact to serialize with `Bitser`.
 *
 * Use `AnimationRenderer.toJOMLMatrix` to convert an `AnimationMatrix` to a `Matrix3x2f`.
 */
@BitStruct(backwardCompatible = true)
class AnimationMatrix(

	// For some reason, I need to divide all flash translations by 20 (and 20 * 0.05 = 1)
	@BitField(id = 0)
	@FloatField(
		expectMultipleOf = 0.05, commonValues = [0.0, -71.5, 100.45, 209.0, -70.9],
		expectedIntegerMultiple = IntegerField(expectUniform = false, digitSize = 3, minValue = -5000, maxValue = 8000),
	)
	val translateX: Float,

	@BitField(id = 1)
	@FloatField(
		expectMultipleOf = 0.05, commonValues = [0.0, 138.5, -151.05, -160.7],
		expectedIntegerMultiple = IntegerField(expectUniform = true, minValue = -4000, maxValue = 6000),
	)
	val translateY: Float,

	@BitField(id = 2)
	@FloatField(
		expectMultipleOf = 0.01, errorTolerance = 0.005, commonValues = [0.0],
		expectedIntegerMultiple = IntegerField(expectUniform = false, digitSize = 2, minValue = -450, maxValue = 150),
	)
	val rotateSkew0: Float,

	@BitField(id = 3)
	@FloatField(
		expectMultipleOf = 0.01, errorTolerance = 0.005, commonValues = [0.0],
		expectedIntegerMultiple = IntegerField(expectUniform = true, minValue = -150, maxValue = 250),
	)
	val rotateSkew1: Float,

	@BitField(id = 4)
	@FloatField(
		expectMultipleOf = 0.002, commonValues = [1.0],
		expectedIntegerMultiple = IntegerField(expectUniform = true, minValue = -1500, maxValue = 4500),
	)
	val scaleX: Float,

	@BitField(id = 5)
	@FloatField(
		expectMultipleOf = 0.002, commonValues = [1.0],
		expectedIntegerMultiple = IntegerField(expectUniform = false, digitSize = 4, minValue = -1500, maxValue = 3000),
	)
	val scaleY: Float,
) {
	constructor() : this(0f, 0f, 0f, 0f, 0f, 0f)

	companion object {
		val DEFAULT = AnimationMatrix(
			0f, 0f, 0f, 0f, 1f, 1f
		)
	}

	override fun equals(other: Any?): Boolean {
		if (other !is AnimationMatrix) return false
		if (this.translateX != other.translateX || this.translateY != other.translateY) return false
		if (this.rotateSkew0 != other.rotateSkew0 || this.rotateSkew1 != other.rotateSkew1) return false
		return this.scaleX == other.scaleX && this.scaleY == other.scaleY
	}

	override fun hashCode() = translateX.hashCode() + 13 * translateY.hashCode() -
			31 * rotateSkew0.hashCode() - 59 * rotateSkew1.hashCode()
}
