package mardek.state.ingame.menu

import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState

class InGameMenuState(private val state: CampaignState) {

	var shown = false
	var currentTab: InGameMenuTab = PartyTab(state)

	fun update(input: InputManager, soundQueue: SoundQueue, assets: Content) {
		while (true) {
			val event = input.consumeEvent() ?: break

			if (event is InputKeyEvent && (event.didPress || event.didRepeat)) {
				if (!currentTab.inside) {
					if (event.key == InputKey.ToggleMenu || event.key == InputKey.Cancel) {
						shown = false
						continue
					}

					if (event.key == InputKey.MoveUp) {
						val oldTab = currentTab
						if (currentTab is SkillsTab) currentTab = PartyTab(state)
						if (currentTab is InventoryTab) currentTab = SkillsTab(assets, state)
						if (currentTab is MapTab) currentTab = InventoryTab(state)

						if (oldTab !== currentTab) soundQueue.insert("menu-scroll")
						continue
					}

					if (event.key == InputKey.MoveDown) {
						val oldTab = currentTab
						if (currentTab is InventoryTab) currentTab = MapTab()
						if (currentTab is SkillsTab) currentTab = InventoryTab(state)
						if (currentTab is PartyTab) currentTab = SkillsTab(assets, state)

						if (oldTab !== currentTab) soundQueue.insert("menu-scroll")
						continue
					}
				}

				currentTab.processKeyPress(event.key, soundQueue)
			}

			if (event is MouseMoveEvent) currentTab.processMouseMove(event)
		}
	}
}
