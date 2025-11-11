package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.Direction
import mardek.content.sprite.DirectionalSprites

/**
 * Represents a non-player character inside an area. Some area characters can walk (randomly), whereas others never
 * move.
 *
 * An `AreaCharacter` that doesn't walk is almost the same as an `AreaObject`. Currently, the difference is that
 * `AreaCharacter`s have *directional* sprites (left, right, etc...), whereas `AreaObject`s have just 1 sprite
 * (or 1 animation).
 */
@BitStruct(backwardCompatible = true)
class AreaCharacter(

	/**
	 * The name of the character (as imported from Flash). It is currently unused.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The sprites of the character, normally 2 sprites per direction .
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "character sprites")
	val sprites: DirectionalSprites,

	/**
	 * The X-coordinate of the tile where this character will start/spawn when the player enters the area.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val startX: Int,

	/**
	 * The Y-coordinate of the tile where this character will start/spawn when the player enters the area.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val startY: Int,

	/**
	 * The direction that this character will face when the player enters the area. Currently, a default direction of
	 * `Down` is assumed when `startDirection == null`.
	 */
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
	 *
	 * When it is -2, this `AreaCharacter` is almost an `AreaObject`, with the only difference that the `AreaCharacter`
	 * will look towards the player when the player talks to it, which an `AreaObject` will never do.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = -2)
	val walkSpeed: Int,

	/**
	 * The element of this character. This is imported from Flash, but currently unused.
	 */
	@BitField(id = 7, optional = true)
	val element: String?,

	/**
	 * The 'conversation name' that was imported from Flash. It is currently unused.
	 */
	@BitField(id = 8, optional = true)
	val conversationName: String?,

	/**
	 * The 'conversation' that was imported from Flash. It is currently unused.
	 */
	@BitField(id = 9, optional = true)
	val rawConversation: String?, // TODO CHAP1 Work this out later

	/**
	 * When `encyclopediaPerson` is non-null and the player interacts with this character, the character named
	 * `encyclopediaPerson` should be added to the encyclopedia (unless already present).
	 */
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

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
