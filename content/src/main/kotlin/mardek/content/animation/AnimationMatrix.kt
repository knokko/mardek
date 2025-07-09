package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = true)
class AnimationMatrix(

	// For some reason, I need to divide all flash translations by 20 (and 20 * 0.05 = 1)
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.05)
	val translateX: Float,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val translateY: Float,

	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew0: Float,

	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew1: Float,

	@BitField(id = 4)
	val hasScale: Boolean,

	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val scaleX: Float,

	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val scaleY: Float,
) {
	constructor() : this(0f, 0f, 0f, 0f, false, 0f, 0f)

	companion object {
		val DEFAULT = AnimationMatrix(
			0f, 0f, 0f, 0f, false, 1f, 1f
		)
	}
}
