package mardek.content

import mardek.content.area.Direction

sealed class Action {
}

// TODO Specify character properly
class ActionWalk(val character: Any, val direction: Direction)

class ActionToggleSwitch(val color: String)
