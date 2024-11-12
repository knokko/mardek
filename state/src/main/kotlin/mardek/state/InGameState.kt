package mardek.state

import mardek.input.InputManager
import mardek.state.ingame.GameProgression
import kotlin.time.Duration

class InGameState(val progress: GameProgression): GameState {
	override fun update(input: InputManager, timeStep: Duration): GameState {
		progress.update(input, timeStep)
		return this
	}
}
