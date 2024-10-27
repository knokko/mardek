package mardek.assets.area.objects

import mardek.assets.area.Direction

class AreaCharacter(
	val name: String,
	val spritesheetName: String,
	val startX: Int,
	val startY: Int,
	val startDirection: Direction?,
	/**
	 * Not sure what this means. It is used by the throne, unactivated Legion, and dead Dracelon
	 */
	val silent: Boolean,
	/**
	 * When walkSpeed is positive, the character randomly moves around. When it's -1, the character stays at the same
	 * position, but still shows the walking animation. When it's -2, the character doesn't move, nor does it show any
	 * walking animation.
	 */
	val walkSpeed: Int,
	val element: String?,
	val conversationName: String?,
	val rawConversation: String?, // TODO Work this out later
	val encyclopediaPerson: String?,
) {

	override fun toString() = "Character($name, $spritesheetName, x=$startX, y=$startY, direction=$startDirection, " +
			"silent=$silent, walkSpeed=$walkSpeed, element=$element, " +
			"conversation=${conversationName ?: rawConversation}, person=$encyclopediaPerson)"

	override fun equals(other: Any?) = other is AreaCharacter && name == other.name &&
			spritesheetName == other.spritesheetName && startX == other.startX && startY == other.startY &&
			startDirection == other.startDirection && silent == other.silent && walkSpeed == other.walkSpeed &&
			element == other.element && conversationName == other.conversationName &&
			rawConversation == other.rawConversation && encyclopediaPerson == other.encyclopediaPerson

	override fun hashCode() = name.hashCode() + spritesheetName.hashCode()
}
