package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.action.ActionSequence
import mardek.content.sprite.ObjectSprites

/**
 * Represents an object in an area.
 * - The player can **not** walk through `AreaObject`s
 * - `AreaObject`s have either a fixed sprite or a simple animation
 * - The player can interact with *some* `AreaObject`s, but not all
 */
@BitStruct(backwardCompatible = true)
class AreaObject(

	/**
	 * The sprite or animation of this object
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	/**
	 * The X-coordinate of the tile occupied by this object
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the tile occupied by this object
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The raw 'conversation name', as imported from Flash. It will always be `null` when this object is not
	 * interactable.
	 */
	@BitField(id = 3, optional = true)
	val conversationName: String?,

	/**
	 * The raw 'conversation', as imported from Flash. It will always be `null` when this object is not
	 * interactable.
	 */
	@BitField(id = 4, optional = true)
	val rawConversion: String?,

	/**
	 * When this object is a sign, this field determines which kind of sign.
	 */
	@BitField(id = 5, optional = true)
	val signType: String?,

	/**
	 * The action sequence that should be activated when the player interacts with this object. Eventually, all
	 * interactable objects should have an action sequence, but we aren't there yet (not even close).
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
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
