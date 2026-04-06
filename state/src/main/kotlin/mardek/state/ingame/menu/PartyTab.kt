package mardek.state.ingame.menu

/**
 * The "Party" tab of the in-game menu. TODO CHAP1 Implement this
 */
class PartyTab: InGameMenuTab() {

	override fun getText() = "Party"

	override fun canGoInside() = false

	override fun shouldShowLowerBar() = true
}
