package mardek.state

import mardek.input.InputManager
import kotlin.time.Duration

class ExitState: GameState {
	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue) = this
}
