package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineBooleanValue
import mardek.content.story.TimelineExpression
import java.util.UUID

/**
 * Represents a dialogue action where the player can *choose* what his character will say (or do). This is mostly used
 * in save crystals, and is otherwise quite rare.
 */
@BitStruct(backwardCompatible = true)
class ChoiceActionNode(

	id: UUID,

	/**
	 * The character whose portrait will be shown when the choices are presented
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val speaker: ActionTarget,

	/**
	 * The options from which the player can choose
	 */
	@BitField(id = 1)
	val options: Array<ChoiceEntry>,
) : ActionNode(id) {

	@Suppress("unused")
	private constructor() : this(
		UUID(0, 0),
		ActionTargetPartyMember(), emptyArray(),
	)

	override fun getDirectChildNodes() = options.mapNotNull { it.next }
}

/**
 * An entry of a `ChoiceActionNode`
 */
@BitStruct(backwardCompatible = true)
class ChoiceEntry(

	/**
	 * The portrait expression (e.g. "norm" or "susp")
	 */
	@BitField(id = 0)
	val expression: String,

	/**
	 * The text of this dialogue choice option
	 */
	@BitField(id = 1)
	val text: String,

	/**
	 * The next action node *when this option is chosen*. When `null`, choosing this option ends the dialogue.
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "action nodes")
	val next: ActionNode?,

	/**
	 * This option is only visible/selectable when this condition evaluates to true.
	 */
	@BitField(id = 3)
	@ClassField(root = TimelineExpression::class)
	val condition: TimelineExpression<Boolean> = ConstantTimelineExpression(
		TimelineBooleanValue(true)
	),
) {

	@Suppress("unused")
	private constructor() : this("", "", null)

	override fun toString() = "Choice($text)"
}
