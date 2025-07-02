package mardek.importer.actions

import mardek.content.action.ActionNode
import mardek.content.action.FixedAction
import mardek.content.action.FixedActionNode

fun fixedActionChain(actions: Array<FixedAction>): ActionNode? {
	var next: ActionNode? = null
	for (action in actions.reversed()) {
		next = FixedActionNode(action, next)
	}
	return next
}
