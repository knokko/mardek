package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.sprites.ObjectSpritesheet

/**
 * An object in an area that does not prevent the player from moving onto its tile.
 * - It may or may not have a sprite
 * - It may or may not give light
 * - It may or may not have a conversation when the player interacts with it
 */
@BitStruct(backwardCompatible = false)
class AreaDecoration(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 2, optional = true)
	val spritesheetName: String?,

	@BitField(ordering = 3, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val spritesheetOffsetY: Int?,

	/**
	 * In pixels
	 */
	@BitField(ordering = 4, optional = true)
	@IntegerField(expectUniform = false, minValue = 16)
	val spriteHeight: Int?,

	@BitField(ordering = 5, optional = true)
	val light: AreaLight?,

	@BitField(ordering = 6)
	@IntegerField(minValue = 1, expectUniform = false)
	val timePerFrame: Int,

	@BitField(ordering = 7, optional = true)
	val rawConversation: String?,

	@BitField(ordering = 8, optional = true)
	val conversationName: String?,
) {

	@BitField(ordering = 9, optional = true)
	var spritesheet: ObjectSpritesheet? = null

	@Suppress("unused")
	private constructor() : this(
		0, 0, null, null,
		null, null, 1, null, null
	)

	override fun toString() = "Decoration(x=$x, y=$y, sheet=$spritesheetName, conversation=$rawConversation)"

	override fun equals(other: Any?) = other is AreaDecoration && x == other.x && y == other.y &&
			spritesheetName == other.spritesheetName && spritesheetOffsetY == other.spritesheetOffsetY &&
			spriteHeight == other.spriteHeight && light == other.light && rawConversation == other.rawConversation &&
			conversationName == other.conversationName

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + spritesheetName.hashCode()
		return result
	}
}
