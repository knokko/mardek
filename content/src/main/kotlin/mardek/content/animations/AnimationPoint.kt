package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class AnimationPoint(
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.05)
	val x: Float,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val y: Float
) {
	internal constructor() : this(0f, 0f)

	override fun toString() = "($x, $y)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
