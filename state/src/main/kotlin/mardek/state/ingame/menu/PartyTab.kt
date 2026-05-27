package mardek.state.ingame.menu

import mardek.input.InputKey

/**
 * The "Party" tab of the in-game menu.
 *
 * This class tracks which of the 7 tabs the player is currently viewing.
 */
class PartyTab: InGameMenuTab() {

	/**
	 * The index of the currently-selected tag (e.g. 0 for Condition and 1 for Vital Statistics)
	 */
	var currentTab = 0
		private set

	override fun getText() = "Party"

	override fun canGoInside() = false

	override fun shouldShowLowerBar() = true

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		super.processKeyPress(key, context)

		if (key == InputKey.MoveLeft) {
			currentTab -= 1
			if (currentTab < 0) currentTab += NUM_TABS
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}
		if (key == InputKey.MoveRight) {
			currentTab = (currentTab + 1) % NUM_TABS
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}
	}

	companion object {

		/**
		 * The number of subtabs that the Party tab has, currently 7
		 */
		const val NUM_TABS = 7
	}
}
