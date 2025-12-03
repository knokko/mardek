package mardek.importer.actions

import com.github.knokko.bitser.Bitser
import mardek.content.action.ActionNode
import mardek.content.action.ActionSequence
import mardek.content.action.ChoiceActionNode
import mardek.content.action.FixedActionNode
import java.nio.ByteBuffer
import java.util.UUID

fun getAllActionNodesFromSequence(actions: ActionSequence): Collection<ActionNode> {
	val allNodes = mutableListOf<ActionNode>()
	val remainingNodes = mutableListOf(actions.root)

	while (remainingNodes.isNotEmpty()) {
		val nextNode = remainingNodes.removeLast()
		allNodes.add(nextNode)

		if (nextNode is FixedActionNode && nextNode.next != null) remainingNodes.add(nextNode.next!!)
		if (nextNode is ChoiceActionNode) {
			for (choice in nextNode.options) {
				if (choice.next != null) remainingNodes.add(choice.next!!)
			}
		}
	}
	return allNodes
}

/**
 * Assigns a unique ID to every node of the given action sequence. They are derived from hash codes, which means that
 * they should normally **not** change after re-exporting the game content.
 */
fun generateUUIDs(actions: ActionSequence) {
	val allNodes = getAllActionNodesFromSequence(actions)

	val bitser = Bitser(false)
	val byteBuffer = ByteBuffer.allocate(4 * allNodes.size)
	for (node in allNodes) byteBuffer.putInt(bitser.hashCode(node))
	val baseID = UUID.nameUUIDFromBytes(byteBuffer.array())

	for ((index, node) in allNodes.withIndex()) {
		node.id = UUID(
			baseID.mostSignificantBits + index.toLong(),
			baseID.leastSignificantBits + index.toLong(),
		)
	}
}
