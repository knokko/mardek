package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * An action sequence is quite literally a sequence of actions (e.g. dialogues). It is currently just a tuple
 * (name, rootNode).
 */
@BitStruct(backwardCompatible = true)
class ActionSequence(

	/**
	 * The name of the action sequence, which is used during importing from flash. Furthermore, the name is occasionally
	 * useful for debugging.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The root node (first node) of the action sequence
	 */
	@BitField(id = 1)
	@ReferenceField(stable = false, label = "action nodes")
	val root: ActionNode,
) {
	internal constructor() : this("", FixedActionNode())

	@BitField(id = 2)
	@Suppress("unused")
	@ClassField(root = ActionNode::class)
	@ReferenceFieldTarget(label = "action nodes")
	private fun getAllNodes() = root.getAllChildNodes()
}
