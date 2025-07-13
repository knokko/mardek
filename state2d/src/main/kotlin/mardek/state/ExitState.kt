package mardek.state

import mardek.input.InputManager

class ExitState: GameState {
	override fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue) {}

	override fun update(context: GameStateUpdateContext) = this
}
