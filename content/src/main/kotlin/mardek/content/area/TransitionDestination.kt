package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.world.WorldMap

/**
 * Represents the destination of a door, portal, or area transition. It can either be a location in an area, or the
 * world map.
 */
@BitStruct(backwardCompatible = true)
class TransitionDestination(
	/**
	 * This field will be non-null if and only if this destination is an area location.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "areas")
	var area: Area?,

	/**
	 * This field will be non-null if and only if this destination goes to the world map. When this is non-null, the
	 * `x`, `y`, and `direction` fields are ignored.
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "world maps")
	var worldMap: WorldMap?,

	/**
	 * When this destination is an area location, this will be the X-coordinate of the destination tile.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = -1, digitSize = 2)
	val x: Int,

	/**
	 * When this destination is an area location, this will be the Y-coordinate of the destination file.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = -1, digitSize = 2)
	val y: Int,

	/**
	 * When this destination is an area location, this will be the direction that the player will face after being
	 * moved to this destination.
	 */
	@BitField(id = 4, optional = true)
	val direction: Direction?,
) {

	internal constructor() : this(null, null, 0, 0, null)

	override fun toString() = "(${area?.properties?.displayName ?: worldMap?.name}, x=$x, y=$y, direction=$direction)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
