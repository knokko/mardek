package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField

@BitStruct(backwardCompatible = true)
class ChoiceActionNode(
	@BitField(id = 0)
	val options: Array<ChoiceEntry>
) : ActionNode() {

}

@BitStruct(backwardCompatible = true)
class ChoiceEntry(

	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val speaker: ActionTarget,

	@BitField(id = 1)
	val expression: String,

	@BitField(id = 2, optional = true)
	@ClassField(root = ActionNode::class)
	val next: ActionNode?,
) {

}
