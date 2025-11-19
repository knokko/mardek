package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
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

	x: Int,
	y: Int,

	/**
	 * The player will be 'moved' to this location after interacting with the door
	 */
	@BitField(id = 1)
	val destination: TransitionDestination,

	/**
	 * The 'type' of lock that this door has, or `null` when this door isn't locked. Note that this hasn't been
	 * implemented yet.
	 */
	@BitField(id = 2, optional = true)
	val lockType: String?,

	/**
	 * Only relevant when lockType == "key"
	 */
	@BitField(id = 3, optional = true)
	val keyName: String?,
) : StaticAreaObject(x, y) {

	@Suppress("unused")
	private constructor() : this(ObjectSprites(), 0, 0, TransitionDestination(), null, null)

	override fun toString() = "${sprites.flashName}(x=$x, y=$y, lockType=$lockType," +
			"${if (keyName != null) " key=$keyName" else ""}, destination=$destination)"
}
