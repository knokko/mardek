package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID

/**
 * A node in an action sequence (technically an action graph, but most 'graphs' are almost linear). Every `ActionNode`
 * contains 1 action (e.g. letting characters talk or walk).
 *
 * Furthermore, every action node contains information to determine the *next* action node. The next action node is
 * usually fixed, but not always (e.g. dialogue choices or plot checks). When the next action node is `null`,
 * the action sequence ends, and the game resumes.
 */
@BitStruct(backwardCompatible = true)
sealed class ActionNode() {

	/**
	 * The unique ID of this node, which is used for (de)serialization.
	 *
	 * It is initially `null`, but the importer should use the `generateUUIDs` method to set them.
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	var id: UUID? = null

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			FixedActionNode::class.java,
			ChoiceActionNode::class.java,
		)
	}
}

/**
 * Represents an `ActionNode` where the *next* node is fixed. This is the most common type of action node.
 */
@BitStruct(backwardCompatible = true)
class FixedActionNode(

	/**
	 * The action to be executed when this node is reached
	 */
	@BitField(id = 0)
	@ClassField(root = FixedAction::class)
	val action: FixedAction,

	/**
	 * The next action node, or `null` to indicate that this is the last node in the action sequence
	 */
	@BitField(id = 1, optional = true)
	@ClassField(root = ActionNode::class)
	@ReferenceFieldTarget(label = "action nodes")
	val next: ActionNode?,
) : ActionNode() {
	constructor() : this(ActionWalk(), null)

	override fun toString() = "FixedNode($action)"
}
