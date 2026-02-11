package mardek.game.action

import mardek.content.area.Direction
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
import mardek.state.ingame.actions.ChatLogEntry
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNotNull
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

	fun testGallovarChoiceChapter1(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_monastery" }!!,
				AreaPosition(2, 5),
				Direction.Right,
			)
			state.campaign.state = areaState

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Gallovar should turn left to face Mardek while he's speaking to him
			val gallovarCharacter = areaState.area.objects.characters.find { it.portrait != null }!!
			var gallovarState = areaState.getCharacterState(gallovarCharacter)!!
			assertEquals(Direction.Left, gallovarState.direction)
			assertEquals(3, gallovarState.x)
			assertEquals(5, gallovarState.y)

			val water = content.stats.elements.find { it.rawName == "WATER" }!!
			val light = content.stats.elements.find { it.rawName == "LIGHT" }!!
			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertSame(water, actions.defaultDialogueObject!!.element)
			assertEquals("Medium Priest Gallovar", actions.defaultDialogueObject!!.displayName)
			assertNotNull(actions.defaultDialogueObject!!.portraitInfo)

			// Skip until the choice dialogue
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(100) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))

			val baseColors = arrayOf(
				Color(50, 50, 50), // YALORT statue
				Color(73, 59, 46), // Floor
			)
			val mardekPortraitColor = arrayOf(Color(102, 102, 0))
			val gallovarPortraitColor = arrayOf(Color(17, 24, 17))
			val chatLogColors = arrayOf(
				Color(255, 255, 255),
				Color(0, 231, 255),
			)
			testRendering(
				state, 1000, 800, "chat-log-off-during-choices",
				baseColors + mardekPortraitColor, gallovarPortraitColor,
			)

			// Choose the second option (ask about YALORT)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Slowly read through the rest of the dialogue
			repeat(2) {
				repeat(100) {
					state.update(updateContext)
				}

				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				state.update(updateContext)
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleChatLog))
			repeat(100) {
				state.update(updateContext)
			}

			testRendering(
				state, 1000, 800, "chat-log-after-choices",
				gallovarPortraitColor + chatLogColors, mardekPortraitColor,
			)

			assertEquals(arrayListOf(
				ChatLogEntry(
					"Medium Priest Gallovar", water,
					"Hello there, lads. May YALORT not smite you this day. ...Can I be of particular assistance?"
				),
				ChatLogEntry("Mardek", light, "Who's YALORT?"),
				ChatLogEntry(
					"Medium Priest Gallovar", water, "Surely you jest, children? " +
							"EVERYONE knows that YALORT is the One True Deity and our eternal benefactor! " +
							"He is the Eternal Dragon who created the world, " +
							"and grants priests such as myself magical power! You should know this, children!"
				),
				ChatLogEntry("Mardek", light, "Well, I was just being silly..."),
			), actions.chatLog)

			// Test that this doesn't crash
			for (characterState in state.campaign.characterStates.values) {
				characterState.currentLevel = 1
			}
			dummySaveManager().createSave(
				content, state.campaign,
				"DoNotCrash", SaveFile.Type.Cheat,
			)

			// Close the dialogue
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			gallovarState = areaState.getCharacterState(gallovarCharacter)!!
			assertEquals(Direction.Down, gallovarState.direction)
			assertEquals(3, gallovarState.x)
			assertEquals(5, gallovarState.y)
		}
	}
}
