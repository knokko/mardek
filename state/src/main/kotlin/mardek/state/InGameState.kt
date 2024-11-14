package mardek.state

import mardek.assets.GameAssets
import mardek.input.InputManager
import mardek.state.ingame.GameProgression
import kotlin.time.Duration

class InGameState(val assets: GameAssets, val progress: GameProgression): GameState {
	override fun update(input: InputManager, timeStep: Duration): GameState {
		progress.update(input, timeStep, assets)
		return this
	}
}
