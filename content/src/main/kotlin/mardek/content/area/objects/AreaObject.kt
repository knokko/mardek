package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.action.ActionSequence
import mardek.content.sprite.ObjectSprites

@BitStruct(backwardCompatible = true)
class AreaObject(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(id = 3, optional = true)
	val conversationName: String?,

	@BitField(id = 4, optional = true)
	val rawConversion: String?,

	@BitField(id = 5, optional = true)
	val signType: String?,

	@BitField(id = 6, optional = true)
	val actionSequence: ActionSequence?,
) {

	@Suppress("unused")
	private constructor() : this(
		ObjectSprites(), 0, 0, null,
		null, null, null
	)

	init {
		if (conversationName != null && rawConversion != null) {
			throw IllegalArgumentException("At most 1 of conversionName and rawConversation can be non-null")
		}
	}

	override fun toString() = "AreaObject(${sprites.flashName}, x=$x, y=$y, " +
			"${rawConversion ?: conversationName}, action=$actionSequence)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
