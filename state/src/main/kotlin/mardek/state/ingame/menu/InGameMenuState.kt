package mardek.state.ingame.menu

import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.inventory.InventoryTab

/**
 * Captures the state of the in-game menu. This tracks whether the in-game menu is currently open, and if so, which
 * tab the player is interacting with. Furthermore, it tracks the 'state' of the opened tab (e.g. which section of the
 * Encyclopedia is shown).
 *
 * Note that it does *not* capture e.g. the inventory state and the toggled skills,
 * which are part of the [CampaignState].
 */
class InGameMenuState(private val state: CampaignState) {

	/**
	 * Whether the in-game menu is currently opened/shown
	 */
	var shown = false

	/**
	 * - When `shown` is `false`, this field is meaningless.
	 * - When `shown` is `true`, this field determines which tab the player is currently viewing,
	 * as well as the 'state' of that tab.
	 */
	var currentTab: InGameMenuTab = PartyTab()

	/**
	 * Updates the state of the in-game menu, and processes all keyboard/mouse events.
	 * This should be invoked during every [mardek.state.ingame.InGameState.update] when `this.shown` is `true`.
	 */
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
