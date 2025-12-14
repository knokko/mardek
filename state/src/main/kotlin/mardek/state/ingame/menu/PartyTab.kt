package mardek.state.ingame.menu

class PartyTab: InGameMenuTab() {

	override fun getText() = "Party"

	override fun canGoInside() = false

	override fun shouldShowLowerBar() = true
}
