package mardek.assets.area.objects

import mardek.assets.area.TransitionDestination

class AreaDoor(
	val spritesheetName: String,
	val spriteRow: Int,
	val x: Int,
	val y: Int,
	val destination: TransitionDestination,
	val lockType: String?,
	/**
	 * Only relevant when lockType == "key"
	 */
	val keyName: String?,
) {

	override fun toString() = "$spritesheetName$spriteRow(x=$x, y=$y, lockType=$lockType," +
			"${if (keyName != null) " key=$keyName" else ""}, destination=$destination)"

	override fun equals(other: Any?) = other is AreaDoor && spritesheetName == other.spritesheetName &&
			spriteRow == other.spriteRow && x == other.x && y == other.y && destination == other.destination &&
			lockType == other.lockType && keyName == other.keyName

	override fun hashCode() = destination.hashCode()
}
