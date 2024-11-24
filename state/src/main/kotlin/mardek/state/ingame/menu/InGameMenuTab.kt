package mardek.state.ingame.menu

abstract class InGameMenuTab(val canGoInside: Boolean) {

	var inside = false

	abstract fun getText(): String
}
