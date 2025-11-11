package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.TransitionDestination
import mardek.content.sprite.ObjectSprites

/**
 * Represents a door in an area. Players use doors to transition to other areas. At least, when the door is not locked.
 */
@BitStruct(backwardCompatible = true)
class AreaDoor(

	/**
	 * The sprites/animation of this door. The first sprite is shown most of the time. The other sprites are only used
	 * when the door is being opened.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	/**
	 * The X-coordinate of the tile where this door is placed.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the tile where this door is placed. When it is a 'high' door, this will be the Y-coordinate
	 * of its 'bottom' tile.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The player will be 'moved' to this location after interacting with the door
	 */
	@BitField(id = 3)
	val destination: TransitionDestination,

	/**
	 * The 'type' of lock that this door has, or `null` when this door isn't locked. Note that this hasn't been
	 * implemented yet.
	 */
	@BitField(id = 4, optional = true)
	val lockType: String?,

	/**
	 * Only relevant when lockType == "key"
	 */
	@BitField(id = 5, optional = true)
	val keyName: String?,
) {

	@Suppress("unused")
	private constructor() : this(ObjectSprites(), 0, 0, TransitionDestination(), null, null)

	override fun toString() = "${sprites.flashName}(x=$x, y=$y, lockType=$lockType," +
			"${if (keyName != null) " key=$keyName" else ""}, destination=$destination)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
