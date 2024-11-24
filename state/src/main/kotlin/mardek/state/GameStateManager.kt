package mardek.state

import mardek.input.InputManager
import kotlin.time.Duration

class GameStateManager(private val input: InputManager, var currentState: GameState) {

	val soundQueue = SoundQueue()

	fun lock(): Any = this

	fun update(timeStep: Duration) {
		this.currentState = this.currentState.update(input, timeStep, soundQueue)
	}
}
