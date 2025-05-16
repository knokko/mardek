package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class AnimationPoint(
	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val x: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val y: Int
) {
	internal constructor() : this(0, 0)

	override fun toString() = "($x, $y)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
