package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.input.MouseMoveEvent

abstract class InGameMenuTab {

	var inside = false

	abstract fun getText(): String

	abstract fun canGoInside(): Boolean

	open fun shouldShowLowerBar() = false

	/**
	 * Whether the clock icon + the in-game time should be rendered in the bottom-left corner.
	 *
	 * This is `true` for most tabs, but there are exceptions (e.g. the Encyclopedia tab in some cases).
	 */
	open fun shouldShowLowerBarClock() = true

	open fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		if (key == InputKey.Interact && canGoInside() && !inside) {
			inside = true
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		}

		if (key == InputKey.Cancel && inside) {
			inside = false
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	}

	open fun processMouseMove(event: MouseMoveEvent, context: UiUpdateContext) {}

	/**
	 * This method should return `true` if and only if the renderer should render the section list (`Party`, `Skills`,
	 * `Inventory`, `Map`, etc...) while the player is viewing this tab.
	 *
	 * This is `true` by default, but some tabs override this when they want to claim the entire window for rendering.
	 */
	open fun shouldShowSectionList() = true
}
