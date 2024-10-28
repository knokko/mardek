package mardek.assets.area.objects

class AreaDecoration(val x: Int, val y: Int, val spritesheetName: String?, val rawConversation: String?) {

	override fun toString() = "Decoration(x=$x, y=$y, sheet=$spritesheetName, conversation=$rawConversation)"

	override fun equals(other: Any?) = other is AreaDecoration && x == other.x && y == other.y &&
			spritesheetName == other.spritesheetName && rawConversation == other.rawConversation

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + spritesheetName.hashCode()
		return result
	}
}
