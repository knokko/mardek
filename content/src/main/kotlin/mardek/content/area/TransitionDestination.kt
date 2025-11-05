package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class TransitionDestination(
	// TODO CHAP1 Optional worldMap
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "areas")
	var area: Area?,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = -1)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = -1)
	val y: Int,

	@BitField(id = 3, optional = true)
	val direction: Direction?,

	@BitField(id = 4, optional = true)
	val discoveredAreaName: String?,
) {

	internal constructor() : this(null, 0, 0, null, null)

	override fun toString() = "(${area?.properties?.displayName}, x=$x, y=$y, direction=$direction)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
