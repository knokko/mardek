package mardek.importer.actions

import mardek.content.action.ActionNode
import mardek.content.action.ActionSequence
import mardek.content.action.ChoiceActionNode
import mardek.content.action.FixedActionNode

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
