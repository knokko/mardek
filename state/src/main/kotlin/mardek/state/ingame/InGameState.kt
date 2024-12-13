package mardek.state.ingame

import mardek.assets.Campaign
import mardek.input.InputManager
import mardek.state.GameState
import mardek.state.SoundQueue
import mardek.state.ingame.menu.InGameMenuState
import kotlin.time.Duration

class InGameState(val assets: Campaign, val campaign: CampaignState): GameState {

	val menu = InGameMenuState()

	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		if (menu.shown) {
			menu.update(input, soundQueue, assets)
		} else {
			campaign.update(input, timeStep, soundQueue, assets)
			if (campaign.shouldOpenMenu) {
				menu.shown = true
				campaign.shouldOpenMenu = false
			}
		}
		return this
	}
}
