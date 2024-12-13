package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class TransitionDestination(
	// TODO Optional worldMap
	@BitField(ordering = 0, optional = true)
	@ReferenceField(stable = false, label = "areas")
	var area: Area?,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = -1)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = -1)
	val y: Int,

	@BitField(ordering = 3, optional = true)
	val direction: Direction?,

	@BitField(ordering = 4, optional = true)
	val discoveredAreaName: String?,
) {

	internal constructor() : this(null, 0, 0, null, null)
	override fun toString() = "(${area?.properties?.displayName}, x=$x, y=$y, direction=$direction)"

	override fun equals(other: Any?) = other is TransitionDestination && area == other.area && x == other.x &&
			y == other.y && direction == other.direction && discoveredAreaName == other.discoveredAreaName

	override fun hashCode(): Int {
		var result = area.hashCode()
		result = 31 * result + x
		result = 31 * result + y
		result = 31 * result + (direction?.hashCode() ?: 0)
		return result
	}
}
