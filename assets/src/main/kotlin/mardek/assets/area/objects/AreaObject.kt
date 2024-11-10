package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.sprites.ObjectSpritesheet

@BitStruct(backwardCompatible = false)
class AreaObject(

	@BitField(ordering = 0)
	val spritesheetName: String,

	/**
	 * When `firstFrameIndex` and `numFrames` are non-null, only frames `[firstFrameIndex, firstFrameIndex + numFrames>` of
	 * the spritesheet will be used. When they are `null`, all frames of the spritesheet will be used.
	 */
	@BitField(ordering = 1, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val firstFrameIndex: Int?,

	@BitField(ordering = 2, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val numFrames: Int?,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 5, optional = true)
	val conversationName: String?,

	@BitField(ordering = 6, optional = true)
	val rawConversion: String?,

	@BitField(ordering = 7, optional = true)
	val signType: String?,
) {

	@BitField(ordering = 8, optional = true)
	var spritesheet: ObjectSpritesheet? = null

	@Suppress("unused")
	private constructor() : this(
		"", null, null, 0, 0,
		null, null, null
	)

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
