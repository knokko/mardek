package mardek.state.ingame.menu

class MapTab: InGameMenuTab(false) {
	override fun getText() = "Map"

	override fun shouldShowLowerBar() = true
}
