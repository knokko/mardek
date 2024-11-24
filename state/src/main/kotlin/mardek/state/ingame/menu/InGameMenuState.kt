package mardek.state.ingame.menu

import mardek.assets.GameAssets
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue

class InGameMenuState {

	var shown = false
	var currentTab: InGameMenuTab = PartyTab()

	fun update(input: InputManager, soundQueue: SoundQueue, assets: GameAssets) {
		while (true) {
			val event = input.consumeEvent() ?: break

			if (event is InputKeyEvent && (event.didPress || event.didRepeat)) {
				if (currentTab.inside) {
					if (event.key == InputKey.Cancel) {
						currentTab.inside = false
						soundQueue.insert("click-cancel")
					}
				} else {
					if (event.key == InputKey.ToggleMenu || event.key == InputKey.Cancel) {
						shown = false
						return
					}

					if (event.key == InputKey.MoveUp) {
						val oldTab = currentTab
						if (currentTab is InventoryTab) currentTab = PartyTab()

						if (oldTab !== currentTab) soundQueue.insert("menu-scroll")
					}

					if (event.key == InputKey.MoveDown) {
						val oldTab = currentTab
						if (currentTab is PartyTab) currentTab = InventoryTab()

						if (oldTab !== currentTab) soundQueue.insert("menu-scroll")
					}

					if (event.key == InputKey.Interact && currentTab.canGoInside) {
						currentTab.inside = true
						soundQueue.insert("click-confirm")
					}
				}
			}
		}
	}
}
