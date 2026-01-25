package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class AreaPosition(

	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val x: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val y: Int
) {

	internal constructor() : this(0, 0)

	override fun toString() = "($x, $y)"

	override fun equals(other: Any?) = other is AreaPosition && this.x == other.x && this.y == other.y

	override fun hashCode() = x - 13 * y
}
