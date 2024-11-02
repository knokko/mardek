package mardek.assets.area

class TransitionDestination(
	val areaName: String,
	val x: Int,
	val y: Int,
	val direction: Direction?,
	val discoveredAreaName: String?,
) {
	override fun toString() = "($areaName, x=$x, y=$y, direction=$direction)"

	override fun equals(other: Any?) = other is TransitionDestination && areaName == other.areaName && x == other.x &&
			y == other.y && direction == other.direction && discoveredAreaName == other.discoveredAreaName

	override fun hashCode(): Int {
		var result = areaName.hashCode()
		result = 31 * result + x
		result = 31 * result + y
		result = 31 * result + (direction?.hashCode() ?: 0)
		return result
	}
}
