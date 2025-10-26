package mardek.state

import mardek.input.InputManager
import mardek.state.saves.SavesFolderManager

class ExitState: GameState {
	override fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue, saves: SavesFolderManager) = this

	override fun update(context: GameStateUpdateContext) = this
}
