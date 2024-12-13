package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.area.TransitionDestination
import mardek.assets.sprite.ObjectSprites

@BitStruct(backwardCompatible = false)
class AreaDoor(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 3)
	val destination: TransitionDestination,

	@BitField(ordering = 4, optional = true)
	val lockType: String?,

	/**
	 * Only relevant when lockType == "key"
	 */
	@BitField(ordering = 5, optional = true)
	val keyName: String?,
) {

	@Suppress("unused")
	private constructor() : this(ObjectSprites(), 0, 0, TransitionDestination(), null, null)

	override fun toString() = "${sprites.flashName}(x=$x, y=$y, lockType=$lockType," +
			"${if (keyName != null) " key=$keyName" else ""}, destination=$destination)"

	override fun equals(other: Any?) = other is AreaDoor && sprites == other.sprites &&
			x == other.x && y == other.y && destination == other.destination &&
			lockType == other.lockType && keyName == other.keyName

	override fun hashCode() = destination.hashCode()
}
