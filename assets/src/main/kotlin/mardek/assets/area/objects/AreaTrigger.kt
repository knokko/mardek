package mardek.assets.area.objects

class AreaTrigger(
	/**
	 * Seems to be unused, but could be useful to keep around for debugging
	 */
	val name: String,
	val x: Int, val y: Int,
	/**
	 * The flash code, mostly useful for development
	 */
	val flashCode: String,
	/**
	 * When true, the trigger can only happen once (per 'campaign'). When false, players can keep triggering it.
	 */
	val oneTimeOnly: Boolean,
	/**
	 * Only used for some complex triggers in the inventors lab. When true, the trigger is not `oneTimeOnly`, but it
	 * can't be triggered again until the player leaves and re-enters the map.
	 */
	val oncePerAreaLoad: Boolean,
	/**
	 * Seems to be true for most teleporting triggers. It means that it only triggers after the player
	 * **reaches** the tile.
	 */
	val walkOn: Boolean
) {

	override fun toString() = "Trigger(x=$x, y=$y, once=$oneTimeOnly, walkOn=$walkOn, code=$flashCode)"

	override fun equals(other: Any?) = other is AreaTrigger && name == other.name && x == other.x && y == other.y &&
			flashCode == other.flashCode && oneTimeOnly == other.oneTimeOnly &&
			oncePerAreaLoad == other.oncePerAreaLoad && walkOn == other.walkOn

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + flashCode.hashCode()
		return result
	}
}
