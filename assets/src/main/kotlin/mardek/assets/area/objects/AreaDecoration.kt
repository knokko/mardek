package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.sprite.ObjectSprites

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
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites?,

	@BitField(ordering = 3, optional = true)
	val light: AreaLight?,

	@BitField(ordering = 4)
	@IntegerField(minValue = 1, expectUniform = false)
	val timePerFrame: Int,

	@BitField(ordering = 5, optional = true)
	val rawConversation: String?,

	@BitField(ordering = 6, optional = true)
	val conversationName: String?,
) {

	@Suppress("unused")
	private constructor() : this(
		0, 0, null, null, 1, null, null
	)

	override fun toString() = "Decoration(x=$x, y=$y, sheet=${sprites?.flashName}, conversation=$rawConversation)"

	override fun equals(other: Any?) = other is AreaDecoration && x == other.x && y == other.y &&
			sprites == other.sprites && light == other.light && rawConversation == other.rawConversation &&
			conversationName == other.conversationName

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + sprites.hashCode()
		return result
	}
}
