package mardek.assets.area.objects

class AreaObject(
	val spritesheetName: String,
	/**
	 * When `firstFrameIndex` and `numFrames` are non-null, only frames `[firstFrameIndex, firstFrameIndex + numFrames>` of
	 * the spritesheet will be used. When they are `null`, all frames of the spritesheet will be used.
	 */
	val firstFrameIndex: Int?,
	val numFrames: Int?,
	val x: Int,
	val y: Int,
	// TODO Walkable boolean, used by dream circles and portals
	val conversationName: String?,
	val rawConversion: String?,
	val signType: String?,
) {
	init {
		if (conversationName != null && rawConversion != null) {
			throw IllegalArgumentException("At most 1 of conversionName and rawConversation can be non-null")
		}
		if ((firstFrameIndex == null) != (numFrames == null)) {
			throw IllegalArgumentException("firstFrameIndex must be null if and only if numFrames is null")
		}
	}

	override fun toString() = "AreaObject($spritesheetName, x=$x, y=$y, ${rawConversion ?: conversationName})"

	override fun equals(other: Any?) = other is AreaObject && spritesheetName == other.spritesheetName &&
			x == other.x && y == other.y && conversationName == other.conversationName &&
			rawConversion == other.rawConversion

	override fun hashCode(): Int {
		var result = spritesheetName.hashCode()
		result = 31 * result + x
		result = 31 * result + y
		result = 31 * result + (conversationName?.hashCode() ?: 0)
		result = 31 * result + (rawConversion?.hashCode() ?: 0)
		return result
	}
}
