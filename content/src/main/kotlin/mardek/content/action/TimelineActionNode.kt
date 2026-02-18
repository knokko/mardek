package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import mardek.content.BITSER
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineActionNodeValue
import mardek.content.story.TimelineExpression
import java.util.UUID

/**
 * A 'fake' [ActionNode] that immediately redirects to the next node. The story/timeline state determines which node
 * that 'next' node is.
 */
@BitStruct(backwardCompatible = true)
class TimelineActionNode(
	id: UUID,

	/**
	 * The timeline expression that determines the next node
	 */
	@BitField(id = 0)
	@ClassField(root = TimelineExpression::class)
	val expression: TimelineExpression<ActionNode?>
): ActionNode(id) {

	@Suppress("unused")
	private constructor() : this(
		UUID(0, 0),
		ConstantTimelineExpression(TimelineActionNodeValue(null)),
	)

	override fun getDirectChildNodes(): Collection<ActionNode> {
		val destination = mutableMapOf<Class<*>, Collection<Any>>()
		destination[ActionNode::class.java] = mutableSetOf()
		BITSER.collectInstances(this, hashMapOf(), destination)
		@Suppress("UNCHECKED_CAST")
		return destination[ActionNode::class.java] as Collection<ActionNode>
	}
}
