package mardek.state.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.*
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.util.Rectangle
import java.io.ByteArrayInputStream

class TitleScreenState: GameState {

	var newGameButton: Rectangle? = null
	var loadGameButton: Rectangle? = null
	var musicPlayerButton: Rectangle? = null
	var quitButton: Rectangle? = null

	var selectedButton = -1

	private val buttons = listOf(::newGameButton, ::loadGameButton, ::musicPlayerButton, ::quitButton)
	private var afterContentLoaded: ((content: Content, soundQueue: SoundQueue) -> GameState)? = null

	private fun update(input: InputManager, context: GameStateUpdateContext?): GameState {
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
					val nextMenu = handleButtonClick()
					if (nextMenu != this) return nextMenu
				}
			}
		}

		return if (context != null && afterContentLoaded != null) {
			afterContentLoaded!!(context.content, context.soundQueue)
		} else this
	}

	override fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue): GameState {
		return update(input, null)
	}

	override fun update(context: GameStateUpdateContext): GameState {
		return update(context.input, context)
	}

	private fun handleButtonClick(): GameState {
		if (selectedButton == 0) {
			afterContentLoaded = { content, soundQueue -> startNewGame(content, soundQueue, "TODO")}
		}
		if (selectedButton == 3) return ExitState()
		return this
	}

	private fun startNewGame(content: Content, soundQueue: SoundQueue, campaignName: String): GameState {
		val rawCheckpoint = content.checkpoints["chapter1"]!!
		val bitInput = BitInputStream(ByteArrayInputStream(rawCheckpoint))
		val campaignState = GameStateManager.bitser.deserialize(
			CampaignState::class.java, bitInput, content, Bitser.BACKWARD_COMPATIBLE
		)
		bitInput.close()

		soundQueue.insert(content.audio.fixedEffects.ui.clickConfirm)
		return InGameState(campaignState, campaignName)
	}
}
