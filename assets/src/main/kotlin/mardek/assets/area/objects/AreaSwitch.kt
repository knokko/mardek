package mardek.assets.area.objects

abstract class AreaSwitch(
	val color: String,
	val x: Int,
	val y: Int,
) {
	override fun toString() = "${this::class.java.simpleName.substring(4)}($color, x=$x, y=$y)"

	override fun equals(other: Any?) = other is AreaSwitch && this::class.java == other::class.java &&
			color == other.color && x == other.x && y == other.y

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + color.hashCode()
		return result
	}
}

class AreaSwitchOrb(color: String, x: Int, y: Int): AreaSwitch(color, x, y)

class AreaSwitchGate(color: String, x: Int, y: Int): AreaSwitch(color, x, y)

class AreaSwitchPlatform(color: String, x: Int, y: Int): AreaSwitch(color, x, y)
