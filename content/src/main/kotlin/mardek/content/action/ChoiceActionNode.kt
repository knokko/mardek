package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineBooleanValue
import mardek.content.story.TimelineExpression

/**
 * Represents a dialogue action where the player can *choose* what his character will say (or do). This is mostly used
 * in save crystals, and is otherwise quite rare.
 */
@BitStruct(backwardCompatible = true)
class ChoiceActionNode(

	/**
	 * The character whose portrait will be shown when the choices are presented
	 */
	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val speaker: ActionTarget,

	/**
	 * The portrait expression (e.g. "norm" or "susp")
	 */
	@BitField(id = 1)
	val expression: String,

	/**
	 * The options from which the player can choose
	 */
	@BitField(id = 2)
	val options: Array<ChoiceEntry>
) : ActionNode() {

	@Suppress("unused")
	private constructor() : this(ActionTargetPartyMember(), "", emptyArray())
}

/**
 * An entry of a `ChoiceActionNode`
 */
@BitStruct(backwardCompatible = true)
class ChoiceEntry(

	/**
	 * The text of this dialogue choice option
	 */
	@BitField(id = 0)
	val text: String,

	/**
	 * The next action node *when this option is chosen*. When `null`, choosing this option ends the dialogue.
	 */
	@BitField(id = 1, optional = true)
	@ClassField(root = ActionNode::class)
	@ReferenceFieldTarget(label = "action nodes")
	val next: ActionNode?,

	/**
	 * This option is only visible/selectable when this condition evaluates to true.
	 */
	@BitField(id = 2)
	@ClassField(root = TimelineExpression::class)
	val condition: TimelineExpression<Boolean> = ConstantTimelineExpression(
		TimelineBooleanValue(true)
	),
) {

	@Suppress("unused")
	private constructor() : this("", null)

	override fun toString() = "Choice($text)"
}
