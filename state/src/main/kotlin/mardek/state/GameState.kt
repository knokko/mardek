package mardek.state

import mardek.input.InputManager
import kotlin.time.Duration

interface GameState {

	fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState
}
