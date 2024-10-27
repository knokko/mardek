package mardek.assets.area.objects

class AreaTalkTrigger(
	val name: String,
	val x: Int,
	val y: Int,
	val npcName: String,
) {

	override fun toString() = "TalkTrigger($name, x=$x, y=$y, npc=$npcName)"

	override fun equals(other: Any?) = other is AreaTalkTrigger && name == other.name && x == other.x &&
			y == other.y && npcName == other.npcName

	override fun hashCode(): Int {
		var result = name.hashCode()
		result = 31 * result + x
		result = 31 * result + y
		result = 31 * result + npcName.hashCode()
		return result
	}
}
