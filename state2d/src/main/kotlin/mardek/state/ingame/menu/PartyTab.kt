package mardek.state.ingame.menu

class PartyTab: InGameMenuTab(false) {

	override fun getText() = "Party"

	override fun shouldShowLowerBar() = true
}
