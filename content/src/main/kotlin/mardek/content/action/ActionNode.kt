package mardek.content.action

sealed class ActionNode

class FixedActionNode(val action: FixedAction, val next: ActionNode?) : ActionNode()

class VariableActionNode : ActionNode()
