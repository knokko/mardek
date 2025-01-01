package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = false)
class AnimationMatrix(

	// For some reason, I need to divide all flash translations by 20 (and 20 * 0.05 = 1)
	@BitField(ordering = 0)
	@FloatField(expectMultipleOf = 0.05)
	val translateX: Float,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.05)
	val translateY: Float,

	@BitField(ordering = 2)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew0: Float,

	@BitField(ordering = 3)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew1: Float,

	@BitField(ordering = 4)
	val hasScale: Boolean,

	@BitField(ordering = 5)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val scaleX: Float,

	@BitField(ordering = 6)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val scaleY: Float,
) {
	constructor() : this(0f, 0f, 0f, 0f, false, 0f, 0f)
}
