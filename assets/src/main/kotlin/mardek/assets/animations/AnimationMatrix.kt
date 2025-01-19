package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AnimationMatrix(
	// TODO Make translation a float field with multiple of 0.05?
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false)
	val translateX: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val translateY: Int,

	// TODO Maybe make skew and scale more compact
	@BitField(ordering = 2)
	@FloatField
	val rotateSkew0: Float,

	@BitField(ordering = 3)
	@FloatField
	val rotateSkew1: Float,

	@BitField(ordering = 4)
	val hasScale: Boolean,

	@BitField(ordering = 5)
	@FloatField
	val scaleX: Float,

	@BitField(ordering = 6)
	@FloatField
	val scaleY: Float,
) {
	constructor() : this(0, 0, 0f, 0f, false, 0f, 0f)
}
