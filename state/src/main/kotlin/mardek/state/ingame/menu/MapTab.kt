package mardek.state.ingame.menu

class MapTab: InGameMenuTab() {
	override fun getText() = "Map"

	override fun canGoInside() = false

	override fun shouldShowLowerBar() = true
}
