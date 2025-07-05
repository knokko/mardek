package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField

@BitStruct(backwardCompatible = true)
sealed class ActionNode {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			FixedActionNode::class.java,
			ChoiceActionNode::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class FixedActionNode(
	@BitField(id = 0)
	@ClassField(root = FixedAction::class)
	val action: FixedAction,

	@BitField(id = 1, optional = true)
	@ClassField(root = ActionNode::class)
	val next: ActionNode?,
) : ActionNode()

