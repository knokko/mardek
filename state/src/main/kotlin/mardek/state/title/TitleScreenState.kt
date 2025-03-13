package mardek.state.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.ExitState
import mardek.state.GameState
import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import java.io.ByteArrayInputStream
import kotlin.time.Duration

class TitleScreenState(val assets: Content): GameState {

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

				if (event.didPress && (event.key == InputKey.Interact || event.key == InputKey.Click)) {
					if (selectedButton == 0) {
						val rawCheckpoint = assets.checkpoints["chapter1"]!!
						val bitInput = BitInputStream(ByteArrayInputStream(rawCheckpoint))
						val campaignState = GameStateManager.bitser.deserialize(
								CampaignState::class.java, bitInput, assets, Bitser.BACKWARD_COMPATIBLE
						)
						bitInput.close()

						soundQueue.insert("click-confirm")
						return InGameState(assets, campaignState)
					}
					if (selectedButton == 3) return ExitState()
				}
			}
		}
		return this
	}
}
