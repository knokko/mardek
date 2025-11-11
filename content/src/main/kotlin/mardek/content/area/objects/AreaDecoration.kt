package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.sprite.ObjectSprites

/**
 * An object in an area that does not prevent the player from moving onto its tile.
 * - It may or may not have a sprite
 * - It may or may not give light
 * - It may or may not have a conversation when the player interacts with it
 */
@BitStruct(backwardCompatible = true)
class AreaDecoration(

	/**
	 * The X-coordinate of the tile where this animation is placed
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the tile where this animation is placed
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The sprite/animation of this decoration, or `null` when it is invisible.
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites?,

	/**
	 * The light that this decoration should emit (e.g. when it is a torch), or `null` when this decoration doesn't
	 * emit any light.
	 */
	@BitField(id = 3, optional = true)
	val light: AreaLight?,

	/**
	 * The duration of one animation frame, in milliseconds
	 */
	@BitField(id = 4)
	@IntegerField(minValue = 1, expectUniform = false)
	val timePerFrame: Int,

	/**
	 * When the player should be able to interact with this decoration, this will be the 'conversation' that was
	 * imported from Flash. It is currently unused.
	 */
	@BitField(id = 5, optional = true)
	val rawConversation: String?,

	/**
	 * When the player should be able to interact with this decoration, this will be the 'conversation name' that was
	 * imported from Flash. It is currently unused.
	 */
	@BitField(id = 6, optional = true)
	val conversationName: String?,
) {

	@Suppress("unused")
	private constructor() : this(
		0, 0, null, null, 1, null, null
	)

	override fun toString() = "Decoration(x=$x, y=$y, sheet=${sprites?.flashName}, conversation=$rawConversation)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
