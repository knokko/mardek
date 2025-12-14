package mardek.state.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.Bitser
import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.input.TextTypeEvent
import mardek.state.*
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.saves.SaveSelectionState
import mardek.state.saves.SavesFolderManager
import mardek.state.util.Rectangle
import java.io.ByteArrayInputStream

class TitleScreenState: GameState {

	var newGameButton: Rectangle? = null
	var loadGameButton: Rectangle? = null
	var musicPlayerButton: Rectangle? = null
	var quitButton: Rectangle? = null

	var beginButton: Rectangle? = null

	var selectedButton = -1

	private val buttons = listOf(::newGameButton, ::loadGameButton, ::musicPlayerButton, ::quitButton, ::beginButton)

	// New-game variables
	var newCampaignName: String? = null
		private set
	private var lastValidatedCampaignName = ""
	var isCampaignNameValid = false
		private set

	// Load-game variables
	var availableCampaigns: Array<String>? = null
		private set
	var saveSelection: SaveSelectionState? = null

	private var afterContentLoaded: ((content: Content, soundQueue: SoundQueue) -> GameState)? = null

	private fun update(input: InputManager, saves: SavesFolderManager, context: GameStateUpdateContext?): GameState {
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
					if (newCampaignName == null && saveSelection == null) {
						if ((event.key == InputKey.MoveUp || event.key == InputKey.CheatScrollUp) && selectedButton >= 0) {
							selectedButton -= 1
						}
						if ((event.key == InputKey.MoveDown || event.key == InputKey.CheatScrollDown) && selectedButton < buttons.size) {
							selectedButton += 1
						}
					}

					if (saveSelection != null) {
						val outcome = saveSelection!!.pressKey(SaveSelectionState.UpdateContext(
							context!!.saves, context.content,
							context.soundQueue, false,
						), event.key)

						if (outcome.canceled) saveSelection = null
						if (outcome.finished) {
							val campaignState = context.saves.loadSave(context.content, outcome.save!!)
							if (campaignState != null) {
								campaignState.markSessionStart()
								return InGameState(campaignState, outcome.save.campaignName)
							}
						}
						if (outcome.finished || outcome.failed) {
							context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickReject)
							saveSelection = null
							availableCampaigns = saves.getCampaignNames()
							return this
						}
					}

					if (newCampaignName != null && newCampaignName!!.isNotEmpty()) {
						if (event.key == InputKey.BackspaceLast) {
							newCampaignName = newCampaignName!!.substring(0 until newCampaignName!!.length - 1)
						}
						if (event.key == InputKey.BackspaceFirst) {
							newCampaignName = newCampaignName!!.substring(1)
						}
					}

					if (event.key == InputKey.Escape) newCampaignName = null
				}

				var didClick = event.key == InputKey.Click
				if (newCampaignName == null) {
					didClick = didClick || event.key == InputKey.Interact
					didClick = didClick || event.key == InputKey.ToggleMenu
				}
				if (event.didPress && didClick && saveSelection == null) {
					val nextMenu = handleButtonClick()
					if (nextMenu != this) return nextMenu
				}

				if (event.key == InputKey.ToggleMenu && newCampaignName != null) tryToStartNewGame()
			}

			if (event is TextTypeEvent && newCampaignName != null) {
				newCampaignName += event.typedText
			}
		}

		if (availableCampaigns == null) availableCampaigns = saves.getCampaignNames()
		if (newCampaignName != null && newCampaignName != lastValidatedCampaignName) {
			isCampaignNameValid = saves.isCampaignNameValid(newCampaignName!!)
			lastValidatedCampaignName = newCampaignName!!
		}

		val result = if (context != null && afterContentLoaded != null) {
			afterContentLoaded!!(context.content, context.soundQueue)
		} else this

		if (result === this && saveSelection != null) {
			val failed = saveSelection!!.update(SaveSelectionState.UpdateContext(
				context!!.saves, context.content, context.soundQueue, false,
			))
			if (failed) {
				context.soundQueue.insert(context.content.audio.fixedEffects.ui.clickReject)
				saveSelection = null
				availableCampaigns = saves.getCampaignNames()
			}
		}

		return result
	}

	override fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue, saves: SavesFolderManager): GameState {
		return update(input, saves, null)
	}

	override fun update(context: GameStateUpdateContext): GameState {
		return update(context.input, context.saves, context)
	}

	private fun handleButtonClick(): GameState {
		if (selectedButton == 0) {
			newCampaignName = ""
			saveSelection = null
			afterContentLoaded = null
		}
		if (selectedButton == 1) {
			newCampaignName = null
			saveSelection = null
			afterContentLoaded = { _, _ ->
				saveSelection = SaveSelectionState(availableCampaigns!!)
				afterContentLoaded = null
				this
			}
		}
		if (selectedButton == 3) return ExitState()
		if (selectedButton == 4) tryToStartNewGame()
		return this
	}

	private fun tryToStartNewGame() {
		if (isCampaignNameValid && lastValidatedCampaignName == newCampaignName) {
			afterContentLoaded = { content, _ ->
				afterContentLoaded = null
				startNewGame(content, newCampaignName!!)
			}
		}
	}

	private fun startNewGame(content: Content, campaignName: String): GameState {
		return StartNewGameState(this, CampaignState.loadChapter(content, 1), campaignName)
	}
}
