package mardek.state

import mardek.input.InputManager
import mardek.state.saves.SavesFolderManager

/**
 * When the game reaches this state, it should close the window and stop the process.
 *
 * This state is reached when the player clicks on the "X" (close button) at the top-right of the window.
 * Furthermore, this state is sometimes reached when the game crashes.
 */
class ExitState: GameState {
	override fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue, saves: SavesFolderManager) = this

	override fun update(context: GameStateUpdateContext) = this
}
