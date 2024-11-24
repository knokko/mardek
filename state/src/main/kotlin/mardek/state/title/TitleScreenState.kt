package mardek.state.title

import mardek.assets.GameAssets
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.ExitState
import mardek.state.GameState
import mardek.state.InGameState
import mardek.state.SoundQueue
import mardek.state.ingame.GameProgression
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharactersState
import kotlin.time.Duration

class TitleScreenState(private val assets: GameAssets): GameState {

	var newGameButton: AbsoluteRectangle? = null
	var loadGameButton: AbsoluteRectangle? = null
	var musicPlayerButton: AbsoluteRectangle? = null
	var quitButton: AbsoluteRectangle? = null

	var selectedButton = -1

	private val buttons = listOf(::newGameButton, ::loadGameButton, ::musicPlayerButton, ::quitButton)

	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		while (true) {
			val event = input.consumeEvent() ?: break

			if (event is MouseMoveEvent) {
				selectedButton = -1
				for ((index, buttonRef) in buttons.withIndex()) {
					val button = buttonRef.get() ?: continue
					if (button.contains(event.newX, event.newY)) selectedButton = index
				}
			}

			if (event is InputKeyEvent) {
				if (event.didPress || event.didRepeat) {
					if (event.key == InputKey.MoveUp && selectedButton >= 0) selectedButton -= 1
					if (event.key == InputKey.MoveDown && selectedButton < buttons.size) selectedButton += 1
				}

				if (event.didPress && event.key == InputKey.Interact) {
					if (selectedButton == 0) {
						val mardek = assets.playableCharacters[0]
						val deugan = assets.playableCharacters[1]
						val startArea = AreaState(
							assets.areas.find { it.properties.rawName == "DL_entr" }!!,
							AreaPosition(3, 3)
						)
						val startCharacters = CharactersState(
							available = mutableSetOf(mardek, deugan),
							unavailable = mutableSetOf(),
							party = arrayOf(mardek, deugan)
						)
						val progress = GameProgression(startArea, startCharacters)
						soundQueue.insert("click-confirm")
						return InGameState(assets, progress)
					}
					if (selectedButton == 3) return ExitState()
				}
			}
		}
		return this
	}
}
