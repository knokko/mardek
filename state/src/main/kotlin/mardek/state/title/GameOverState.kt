package mardek.state.title

import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext

/**
 * The **Game Over** state, which is reached e.g. when the player loses a battle.
 *
 * When the game reaches this state, the player can go to the Title Screen, or exit the game.
 */
class GameOverState: GameState {

	/**
	 * The result of `System.nanoTime()` when the game transitioned to this state. This field is used by the renderer
	 * to render the fade-in of the Game Over screen.
	 */
	val startTime = System.nanoTime()

	override fun update(context: GameStateUpdateContext): GameState {
		while (true) {
			val event = context.input.consumeEvent() ?: break
			if (event is InputKeyEvent && event.didPress) {
				if (event.key == InputKey.Interact || event.key == InputKey.Cancel) return TitleScreenState()
			}
		}

		return this
	}
}
