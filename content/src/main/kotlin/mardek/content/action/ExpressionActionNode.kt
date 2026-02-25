package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import mardek.content.BITSER
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionActionNodeValue
import mardek.content.expression.StateExpression
import java.util.UUID

/**
 * A 'fake' [ActionNode] that immediately redirects to the next node. The campaign state determines which node
 * that 'next' node is.
 */
@BitStruct(backwardCompatible = true)
class ExpressionActionNode(
	id: UUID,

	/**
	 * The state expression that determines the next node
	 */
	@BitField(id = 0)
	@ClassField(root = StateExpression::class)
	val expression: StateExpression<ActionNode?>
): ActionNode(id) {

	@Suppress("unused")
	private constructor() : this(
		UUID(0, 0),
		ConstantStateExpression(ExpressionActionNodeValue(null)),
	)

	override fun getDirectChildNodes(): Collection<ActionNode> {
		val destination = mutableMapOf<Class<*>, Collection<Any>>()
		destination[ActionNode::class.java] = mutableSetOf()
		BITSER.collectInstances(this, hashMapOf(), destination)
		@Suppress("UNCHECKED_CAST")
		return destination[ActionNode::class.java] as Collection<ActionNode>
	}
}
