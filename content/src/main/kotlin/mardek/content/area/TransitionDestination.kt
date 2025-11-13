package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

/**
 * Represents the destination of a door, portal, or area transition. It can either be a location in an area, or the
 * world map.
 */
@BitStruct(backwardCompatible = true)
class TransitionDestination(
	// TODO CHAP1 Optional worldMap
	/**
	 * This field will be non-null if and only if this destination is an area location.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "areas")
	var area: Area?,

	/**
	 * When this destination is an area location, this will be the X-coordinate of the destination tile.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = -1)
	val x: Int,

	/**
	 * When this destination is an area location, this will be the Y-coordinate of the destination file.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = -1)
	val y: Int,

	/**
	 * When this destination is an area location, this will be the direction that the player will face after being
	 * moved to this destination.
	 */
	@BitField(id = 3, optional = true)
	val direction: Direction?,

	/**
	 * When non-null, the area with this name should be added to the encyclopedia (unless it was already in there).
	 */
	@BitField(id = 4, optional = true)
	val discoveredAreaName: String?,
) {

	internal constructor() : this(null, 0, 0, null, null)

	override fun toString() = "(${area?.properties?.displayName}, x=$x, y=$y, direction=$direction)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
