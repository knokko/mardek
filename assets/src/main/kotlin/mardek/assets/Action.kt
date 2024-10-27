package mardek.assets

import mardek.assets.area.Direction

sealed class Action {
}

// TODO Specify character properly
class ActionWalk(val character: Any, val direction: Direction)

class ActionToggleSwitch(val color: String)
