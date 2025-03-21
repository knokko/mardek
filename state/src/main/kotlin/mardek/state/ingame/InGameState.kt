package mardek.state.ingame

import mardek.content.Content
import mardek.input.InputManager
import mardek.state.GameState
import mardek.state.SoundQueue
import mardek.state.ingame.menu.InGameMenuState
import kotlin.time.Duration

class InGameState(val content: Content, val campaign: CampaignState): GameState {

	val menu = InGameMenuState(campaign)

	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		if (menu.shown) {
			menu.update(input, soundQueue, content)
		} else {
			campaign.update(input, timeStep, soundQueue, content)
			if (campaign.shouldOpenMenu) {
				menu.shown = true
				campaign.shouldOpenMenu = false
			}
		}
		return this
	}
}
