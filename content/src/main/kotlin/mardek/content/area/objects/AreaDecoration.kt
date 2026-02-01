package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.action.ActionNode
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
	@IntegerField(minValue = 1, expectUniform = false, commonValues = [150])
	val timePerFrame: Int,

	/**
	 * The action that should be activated when the player interacts with this decoration, or `null` when the
	 * player cannot interact with this decoration (or I haven't implemented it yet), or when `sharedActionSequence`
	 * is used instead.
	 *
	 * This action node is 'owned' by this decoration, and *not* shared with anything else.
	 */
	@BitField(id = 4, optional = true)
	@ClassField(root = ActionNode::class)
	@ReferenceFieldTarget(label = "action nodes")
	val ownActions: ActionNode?,

	/**
	 * When the player should be able to interact with this decoration, this will be the 'conversation name' that was
	 * imported from Flash. It is currently unused.
	 */
	@BitField(id = 5, optional = true)
	val conversationName: String?,

	/**
	 * The action sequence that should be activated when the player interacts with this decoration, or `null` when the
	 * player cannot interact with this decoration (or I haven't implemented it yet), or when `ownActions` is
	 * used instead.
	 *
	 * This must be a reference to either an area action sequence, or to a global action sequence.
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
	val sharedActionSequence: ActionSequence?,

	/**
	 * When this object is a sign, this field determines which kind of sign.
	 */
	@BitField(id = 7, optional = true)
	val signType: String?,

	/**
	 * The display name of this decoration, which is only used when the player starts a dialogue with this decoration.
	 */
	@BitField(id = 8, optional = true)
	val displayName: String?,
) : StaticAreaObject(x, y) {

	@Suppress("unused")
	private constructor() : this(
		0, 0, null, false, null, 1,
		null, null, null, null, null,
	)

	override fun toString() = "Decoration(x=$x, y=$y, sheet=${sprites?.flashName}, name=$displayName)"
}
