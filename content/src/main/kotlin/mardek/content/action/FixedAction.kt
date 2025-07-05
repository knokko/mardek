package mardek.content.action

sealed class FixedAction

class ActionWalk(val target: ActionTarget, val destinationX: Int, val destinationY: Int, val speed: WalkSpeed) : FixedAction()

class ActionTalk(val speaker: ActionTarget, val expression: String, val text: String) : FixedAction()

class ActionBattle() : FixedAction()
