package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.area.Direction
import mardek.assets.sprite.DirectionalSprites

@BitStruct(backwardCompatible = true)
class AreaCharacter(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "character sprites")
	val sprites: DirectionalSprites,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val startX: Int,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val startY: Int,

	@BitField(id = 4, optional = true)
	val startDirection: Direction?,

	/**
	 * Not sure what this means. It is used by the throne, unactivated Legion, and dead Dracelon
	 */
	@BitField(id = 5)
	val silent: Boolean,

	/**
	 * When walkSpeed is positive, the character randomly moves around. When it's -1, the character stays at the same
	 * position, but still shows the walking animation. When it's -2, the character doesn't move, nor does it show any
	 * walking animation.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = -2)
	val walkSpeed: Int,

	@BitField(id = 7, optional = true)
	val element: String?,

	@BitField(id = 8, optional = true)
	val conversationName: String?,

	@BitField(id = 9, optional = true)
	val rawConversation: String?, // TODO Work this out later

	@BitField(id = 10, optional = true)
	val encyclopediaPerson: String?,
) {

	@Suppress("unused")
	private constructor() : this(
		"", DirectionalSprites(), 0, 0, null, false, 0,
		null, null, null, null
	)

	override fun toString() = "Character($name, ${sprites.name}, x=$startX, y=$startY, direction=$startDirection, " +
			"silent=$silent, walkSpeed=$walkSpeed, element=$element, " +
			"conversation=${conversationName ?: rawConversation}, person=$encyclopediaPerson)"

	override fun equals(other: Any?) = other is AreaCharacter && name == other.name &&
			sprites == other.sprites && startX == other.startX && startY == other.startY &&
			startDirection == other.startDirection && silent == other.silent && walkSpeed == other.walkSpeed &&
			element == other.element && conversationName == other.conversationName &&
			rawConversation == other.rawConversation && encyclopediaPerson == other.encyclopediaPerson

	override fun hashCode() = name.hashCode() + sprites.hashCode()
}
