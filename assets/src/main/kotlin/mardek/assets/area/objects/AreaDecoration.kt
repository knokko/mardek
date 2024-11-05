package mardek.assets.area.objects

/**
 * An object in an area that does not prevent the player from moving onto its tile.
 * - It may or may not have a sprite
 * - It may or may not give light
 * - It may or may not have a conversation when the player interacts with it
 */
class AreaDecoration(
	val x: Int, val y: Int,
	val spritesheetName: String?, val spritesheetOffsetY: Int?,
	/**
	 * In pixels
	 */
	val spriteHeight: Int?,
	val light: AreaLight?,
	val rawConversation: String?
) {

	override fun toString() = "Decoration(x=$x, y=$y, sheet=$spritesheetName, conversation=$rawConversation)"

	override fun equals(other: Any?) = other is AreaDecoration && x == other.x && y == other.y &&
			spritesheetName == other.spritesheetName && spritesheetOffsetY == other.spritesheetOffsetY &&
			spriteHeight == other.spriteHeight && light == other.light && rawConversation == other.rawConversation

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + spritesheetName.hashCode()
		return result
	}
}
