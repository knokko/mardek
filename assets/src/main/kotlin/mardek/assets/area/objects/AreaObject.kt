package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.sprite.ObjectSprites

@BitStruct(backwardCompatible = false)
class AreaObject(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 3, optional = true)
	val conversationName: String?,

	@BitField(ordering = 4, optional = true)
	val rawConversion: String?,

	@BitField(ordering = 5, optional = true)
	val signType: String?,
) {

	@Suppress("unused")
	private constructor() : this(
		ObjectSprites(), 0, 0, null, null, null
	)

	init {
		if (conversationName != null && rawConversion != null) {
			throw IllegalArgumentException("At most 1 of conversionName and rawConversation can be non-null")
		}
	}

	override fun toString() = "AreaObject(${sprites.flashName}, x=$x, y=$y, ${rawConversion ?: conversationName})"

	override fun equals(other: Any?) = other is AreaObject && sprites == other.sprites &&
			x == other.x && y == other.y && conversationName == other.conversationName &&
			rawConversion == other.rawConversion

	override fun hashCode(): Int {
		var result = sprites.hashCode()
		result = 31 * result + x
		result = 31 * result + y
		result = 31 * result + (conversationName?.hashCode() ?: 0)
		result = 31 * result + (rawConversion?.hashCode() ?: 0)
		return result
	}
}
