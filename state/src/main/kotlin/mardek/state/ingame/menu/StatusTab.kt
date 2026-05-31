package mardek.state.ingame.menu

/**
 * The "Status" tab of the in-game menu.
 *
 * This class doesn't track any other state.
 */
class StatusTab: InGameMenuTab() {

	override fun getText() = "Status"

	override fun canGoInside() = false
}