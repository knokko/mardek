package mardek.state.ingame.menu

import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue

abstract class InGameMenuTab(val canGoInside: Boolean) {

	var inside = false

	abstract fun getText(): String

	open fun processKeyPress(key: InputKey, soundQueue: SoundQueue) {
		if (key == InputKey.Interact && canGoInside && !inside) {
			inside = true
			soundQueue.insert("click-confirm")
		}

		if (key == InputKey.Cancel && inside) {
			inside = false
			soundQueue.insert("click-cancel")
		}
	}

	open fun processMouseMove(event: MouseMoveEvent) {}
}
