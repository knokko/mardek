package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.input.MouseMoveEvent

abstract class InGameMenuTab(private val canGoInside: Boolean) {

	var inside = false

	abstract fun getText(): String

	open fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		if (key == InputKey.Interact && canGoInside && !inside) {
			inside = true
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		}

		if (key == InputKey.Cancel && inside) {
			inside = false
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	}

	open fun processMouseMove(event: MouseMoveEvent, context: UiUpdateContext) {}
}
