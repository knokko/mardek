package mardek.state.ingame.menu

/**
 * The "Map" tab of the in-game menu.
 *
 * This class doesn't track any other state.
 */
class MapTab: InGameMenuTab() {

	override fun getText() = "Map"

	override fun canGoInside() = false

	override fun shouldShowLowerBar() = true
}
