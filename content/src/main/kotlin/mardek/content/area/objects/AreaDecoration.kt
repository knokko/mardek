package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.ActionSequence
import mardek.content.sprite.ObjectSprites

/**
 * An object in an area that does not move.
 * - It may or may not have a sprite
 * - It may or may not give light
 * - It may or may not allow the player to move through it
 * - It may or may not have a conversation when the player interacts with it
 */
@BitStruct(backwardCompatible = true)
class AreaDecoration(

	x: Int,
	y: Int,

	/**
	 * The sprite/animation of this decoration, or `null` when it is invisible.
	 */
	@BitField(id = 0, optional = true)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites?,

	/**
	 * Whether players can walk through this decoration. Note that even when this is `true`, the player can only walk
	 * through the decoration when the tile below it is accessible.
	 */
	@BitField(id = 1)
	val canWalkThrough: Boolean,

	/**
	 * The light that this decoration should emit (e.g. when it is a torch), or `null` when this decoration doesn't
	 * emit any light.
	 */
	@BitField(id = 2, optional = true)
	val light: AreaLight?,

	/**
	 * The duration of one animation frame, in milliseconds
	 */
	@BitField(id = 3)
	@IntegerField(minValue = 1, expectUniform = false)
	val timePerFrame: Int,

	/**
	 * When the player should be able to interact with this decoration, this will be the 'conversation' that was
	 * imported from Flash. It is currently unused.
	 */
	@BitField(id = 4, optional = true)
	val rawConversation: String?,

	/**
	 * When the player should be able to interact with this decoration, this will be the 'conversation name' that was
	 * imported from Flash. It is currently unused.
	 */
	@BitField(id = 5, optional = true)
	val conversationName: String?,

	/**
	 * The action sequence that should be activated when the player interacts with this decoration, or `null` when the
	 * player cannot interact with this decoration (or I haven't implemented it yet).
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
	val actionSequence: ActionSequence?,

	/**
	 * When this object is a sign, this field determines which kind of sign.
	 */
	@BitField(id = 7, optional = true)
	val signType: String?,
) : StaticAreaObject(x, y) {

	@Suppress("unused")
	private constructor() : this(
		0, 0, null, false, null, 1,
		null, null, null, null,
	)

	override fun toString() = "Decoration(x=$x, y=$y, sheet=${sprites?.flashName}, conversation=$rawConversation)"
}
