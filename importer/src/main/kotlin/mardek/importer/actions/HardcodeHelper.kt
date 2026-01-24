package mardek.importer.actions

import mardek.content.action.ActionNode
import mardek.content.action.FixedAction
import mardek.content.action.FixedActionNode
import java.util.UUID

fun fixedActionChain(actions: Array<FixedAction>, ids: Array<UUID>): ActionNode? {
	if (actions.size != ids.size) throw IllegalArgumentException(
		"There are ${actions.size} actions and ${ids.size} IDs: they must be equal"
	)
	var next: ActionNode? = null
	for ((index, action) in actions.withIndex().reversed()) {
		next = FixedActionNode(ids[index], action, next)
	}
	return next
}
