package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.action.ActionSequence
import mardek.content.area.AreaTransitionDestination
import mardek.content.area.TransitionDestination
import mardek.content.sprite.ObjectSprites
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionBooleanValue
import mardek.content.expression.StateExpression
import java.util.UUID

/**
 * Represents a door in an area. Players use doors to transition to other areas. At least, when the door is not locked.
 */
@BitStruct(backwardCompatible = true)
class AreaDoor(

	/**
	 * The unique ID of this door, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The sprites/animation of this door. The first sprite is shown most of the time. The other sprites are only used
	 * when the door is being opened.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "object sprites")
	val sprites: ObjectSprites,

	x: Int,
	y: Int,

	/**
	 * The player will be 'moved' to this location after interacting with the door
	 */
	@BitField(id = 2)
	@ClassField(root = TransitionDestination::class)
	val destination: TransitionDestination,

	/**
	 * Whether the player is allowed to open this door, which may depend on the state of the story/timeline
	 * (e.g. the door might be locked at night, or require a Plot Item to open).
	 */
	@BitField(id = 3)
	@ClassField(root = StateExpression::class)
	val canOpen: StateExpression<Boolean>,

	/**
	 * The action sequence that should be activated whenever [canOpen] yields `false`. This must be non-null, unless
	 * [canOpen] always yields `true`.
	 */
	@BitField(id = 4, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
	val cannotOpenActions: ActionSequence?,

	/**
	 * The display name of the door, as imported from Flash. This is used in locked door dialogues.
	 */
	@BitField(id = 5)
	val displayName: String,
) : StaticAreaObject(x, y) {

	constructor() : this(
		UUID.randomUUID(), ObjectSprites(), 0, 0, AreaTransitionDestination(),
		ConstantStateExpression(ExpressionBooleanValue(true)),
		null, "",
	)

	override fun toString() = "${sprites.flashName}(x=$x, y=$y, " +
			"actions=${cannotOpenActions?.name}, destination=$destination, name=$displayName)"
}
