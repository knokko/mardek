package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.TransitionDestination
import mardek.assets.area.sprites.ObjectSpritesheet

@BitStruct(backwardCompatible = false)
class AreaDoor(

	@BitField(ordering = 0)
	val spritesheetName: String,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val spriteRow: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 4)
	val destination: TransitionDestination,

	@BitField(ordering = 5, optional = true)
	val lockType: String?,

	/**
	 * Only relevant when lockType == "key"
	 */
	@BitField(ordering = 6, optional = true)
	val keyName: String?,
) {

	@BitField(ordering = 7, optional = true)
	var spritesheet: ObjectSpritesheet? = null

	@Suppress("unused")
	private constructor() : this("", 0, 0, 0, TransitionDestination(), null, null)

	override fun toString() = "$spritesheetName$spriteRow(x=$x, y=$y, lockType=$lockType," +
			"${if (keyName != null) " key=$keyName" else ""}, destination=$destination)"

	override fun equals(other: Any?) = other is AreaDoor && spritesheetName == other.spritesheetName &&
			spriteRow == other.spriteRow && x == other.x && y == other.y && destination == other.destination &&
			lockType == other.lockType && keyName == other.keyName

	override fun hashCode() = destination.hashCode()
}
