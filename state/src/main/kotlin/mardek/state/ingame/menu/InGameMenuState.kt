package mardek.state.ingame.menu

import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.inventory.InventoryTab

class InGameMenuState(private val state: CampaignState) {

	var shown = false
	var currentTab: InGameMenuTab = PartyTab()

	fun update(input: InputManager, soundQueue: SoundQueue, content: Content) {
		val context = UiUpdateContext(
			state.usedPartyMembers(), state.allPartyMembers(), soundQueue,
			content.audio.fixedEffects, content.skills,
			{ state.cursorItemStack },
			{ newCursorStack -> state.cursorItemStack = newCursorStack },
		)
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
						if (currentTab is SkillsTab) currentTab = PartyTab()
						if (currentTab is InventoryTab) currentTab = SkillsTab(state.usedPartyMembers())
						if (currentTab is MapTab) currentTab = InventoryTab()
						if (currentTab is QuestsTab) currentTab = MapTab()
						if (currentTab is EncyclopediaTab) {
							currentTab = QuestsTab(state.story.getQuests(content.story))
						}
						if (currentTab is VideoSettingsTab) {
							val snapshot = state.encyclopedia.createSnapshot(content.encyclopedia, state)
							currentTab = EncyclopediaTab(snapshot)
						}

						if (oldTab !== currentTab) soundQueue.insert(content.audio.fixedEffects.ui.scroll1)
						continue
					}

					if (event.key == InputKey.MoveDown) {
						val oldTab = currentTab
						if (currentTab is EncyclopediaTab) currentTab = VideoSettingsTab()
						if (currentTab is QuestsTab) {
							val snapshot = state.encyclopedia.createSnapshot(content.encyclopedia, state)
							currentTab = EncyclopediaTab(snapshot)
						}
						if (currentTab is MapTab) currentTab = QuestsTab(state.story.getQuests(content.story))
						if (currentTab is InventoryTab) currentTab = MapTab()
						if (currentTab is SkillsTab) currentTab = InventoryTab()
						if (currentTab is PartyTab) currentTab = SkillsTab(state.usedPartyMembers())

						if (oldTab !== currentTab) soundQueue.insert(content.audio.fixedEffects.ui.scroll1)
						continue
					}
				}

				currentTab.processKeyPress(event.key, context)
			}

			if (event is MouseMoveEvent) currentTab.processMouseMove(event, context)
		}
	}

	/**
	 * This method should be called when the player (re-)opens the in-game menu because some tabs (e.g. quests and
	 * encyclopedia) may need to be refreshed.
	 */
	fun refreshCurrentTab(content: Content) {
		if (currentTab is QuestsTab) {
			currentTab = QuestsTab(state.story.getQuests(content.story))
		}
		if (currentTab is EncyclopediaTab) {
			val snapshot = state.encyclopedia.createSnapshot(content.encyclopedia, state)
			currentTab = EncyclopediaTab(snapshot)
		}
	}
}
