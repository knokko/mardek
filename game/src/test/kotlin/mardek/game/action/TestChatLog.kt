package mardek.game.action

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestChatLog {

	fun testSequential(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState.loadChapter(content, 1), "")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds)

			context.input.postEvent(pressKeyEvent(InputKey.Interact)) // Skip chapter number
			context.input.postEvent(repeatKeyEvent(InputKey.Interact)) // Skip intro cutscene
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Skip walking & area fade-in
			repeat(10) {
				state.update(context)
			}

			// Skip dialogue nodes until we reach the last one
			repeat(11) {
				context.input.postEvent(pressKeyEvent(InputKey.Interact))
				context.input.postEvent(releaseKeyEvent(InputKey.Interact))
				repeat(200) {
					state.update(context)
				}
				context.input.postEvent(pressKeyEvent(InputKey.Interact))
				context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			}

			// Open chat log
			context.input.postEvent(pressKeyEvent(InputKey.ToggleChatLog))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleChatLog))
			state.update(context)

			val dimmedAreaColors = arrayOf(
				Color(47, 93, 115), // Save crystal 'ring'
				Color(43, 24, 40), // Brazier foot color
			)
			val normalAreaColors = arrayOf(
				Color(96, 199, 242), // Save crystal 'ring'
				Color(86, 50, 86), // Brazier foot color
			)
			val dialogueColors = arrayOf(
				Color(70, 117, 33), // Deugan cape color
				Color(88, 71, 36), // Chat log button color
				Color(238, 203, 127), // Speaker name color
			)
			val boldChatLogColor = arrayOf(
				Color(253, 218, 117),
			)
			val chatLogColors = arrayOf(
				Color(255, 255, 255), // Mardek name color
				Color(0, 255, 0), // Deugan name color
				Color(186, 146, 77), // Base text color
			) + boldChatLogColor
			testRendering(
				state, 800, 600, "chat-log-open",
				dimmedAreaColors + dialogueColors + chatLogColors,
				normalAreaColors,
			)

			// Close chat log
			context.input.postEvent(pressKeyEvent(InputKey.ToggleChatLog))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleChatLog))
			state.update(context)
			testRendering(
				state, 800, 600, "chat-log-closed",
				dialogueColors + normalAreaColors,
				dimmedAreaColors + boldChatLogColor,
			)
		}
	}
}
