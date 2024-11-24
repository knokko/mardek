package mardek.state.ingame

import mardek.assets.GameAssets
import mardek.input.InputManager
import mardek.state.GameState
import mardek.state.SoundQueue
import mardek.state.ingame.menu.InGameMenuState
import kotlin.time.Duration

class InGameState(val assets: GameAssets, val progress: GameProgression): GameState {

	val menu = InGameMenuState()

	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		if (menu.shown) {
			menu.update(input, soundQueue, assets)
		} else {
			progress.update(input, timeStep, soundQueue, assets)
			if (progress.shouldOpenMenu) {
				menu.shown = true
				progress.shouldOpenMenu = false
			}
		}
		return this
	}
}
