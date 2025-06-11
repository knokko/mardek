package mardek.state.title

import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext

class GameOverState: GameState {

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
