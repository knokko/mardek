package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.input.MouseMoveEvent

/**
 * An 'instance' of one of the tabs of the in-game menu ("Party", "Skills", "Inventory", etc...). An instance of
 * a subclass of this class will be created whenever the player scrolls to a different tab.
 *
 * (So an instance of `InventoryTab` is created whenever the player scrolls from "Skills" to "Inventory", or from
 * "Map" to "Inventory").
 */
abstract class InGameMenuTab {

	/**
	 * Whether the player has 'dived inside' this tab.
	 *
	 * - When `inside` is `false`, the player can switch to a different tab by pressing the up/down arrows.
	 * - When `inside` is `true`, using the up/down arrows will do something else (e.g. switch to a different player
	 * in the inventory tab).
	 *
	 * This field is `false` by default, but the player can 'dive inside' by pressing the Interact key in a tab whose
	 * [canGoInside] returns `true` (e.g. "Skills" and "Inventory", but not "Party").
	 */
	var inside = false

	/**
	 * Gets the text/title of this tab, e.g. "Party" or "Inventory".
	 *
	 * This will be rendered in the 'tab list' on the right of the in-game menu. Furthermore, the text/title of the
	 * currently-active tab is rendered on the top bar.
	 */
	abstract fun getText(): String

	/**
	 * Whether the player can set [inside] to `true` by pressing the Interact key.
	 */
	abstract fun canGoInside(): Boolean

	/**
	 * Whether the renderer should render the dark-brown lower bar when the player is browsing this tab.
	 */
	open fun shouldShowLowerBar() = false

	/**
	 * Whether the clock icon + the in-game time should be rendered in the bottom-left corner.
	 *
	 * This is `true` for most tabs, but there are exceptions (e.g. the Encyclopedia tab in some cases).
	 */
	open fun shouldShowLowerBarClock() = true

	/**
	 * This method should be called on the currently-open tab for each [mardek.input.InputKeyEvent]
	 * with [mardek.input.InputKeyEvent.didPress] = true polled during [InGameMenuState.update].
	 */
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

	/**
	 * This method should be called on the currently-open tab for each [mardek.input.MouseMoveEvent]
	 * polled during [InGameMenuState.update].
	 */
	open fun processMouseMove(event: MouseMoveEvent, context: UiUpdateContext) {}

	/**
	 * This method should return `true` if and only if the renderer should render the section list (`Party`, `Skills`,
	 * `Inventory`, `Map`, etc...) while the player is viewing this tab.
	 *
	 * This is `true` by default, but some tabs override this when they want to claim the entire window for rendering.
	 */
	open fun shouldShowSectionList() = true
}
