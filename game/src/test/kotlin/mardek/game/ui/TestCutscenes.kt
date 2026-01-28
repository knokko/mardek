package mardek.game.ui

import mardek.content.action.ActionShowChapterName
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionWalk
import mardek.content.action.FixedActionNode
import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.action.TestActions.dialogueBoxColors
import mardek.game.action.TestActions.eButtonColors
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.TextTypeEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.title.StartNewGameState
import mardek.state.title.TitleScreenState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.arrayOf
import kotlin.time.Duration.Companion.milliseconds

object TestCutscenes {

	private val titleScreenColors = arrayOf(
			Color(190, 144, 95), // Title outer outline
			Color(242, 183, 113), // Subtitle upper color
			Color(184, 130, 61), // Subtitle lower color
			Color(255, 204, 153), // Button border color
	)

	fun testChapter1Intro(instance: TestingInstance) {
		instance.apply {
			val titleState = TitleScreenState()
			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(),
				10.milliseconds, dummySaveManager(),
			)

			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			titleState.update(context)

			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			titleState.update(context)

			context.input.postEvent(TextTypeEvent("new"))
			titleState.update(context)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			val startState = titleState.update(context) as StartNewGameState

			Thread.sleep(StartNewGameState.FADE_DURATION * 9 / 10 / 1000_000L)
			testRendering(
				startState, 800, 500, "intro-cutscene0",
				TestTitleScreen.titleBarColors, titleScreenColors,
			)

			val igState = startState.update(context) as InGameState

			val chapterNameColors = arrayOf(
				Color(100, 66, 0),
				Color(241, 226, 188),
				Color(232, 198, 124),
				Color(0, 0, 0, 0),
			)
			Thread.sleep(ActionShowChapterName.FADE_DURATION / 1000_000L)
			testRendering(
				igState, 800, 500, "intro-cutscene1",
				chapterNameColors + TestTitleScreen.titleBarColors,
				titleScreenColors,
			)

			context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			igState.update(context)

			val cutsceneColors = arrayOf(
				Color(248, 248, 255), // The sun
				Color(0, 0, 0), // The castle
				Color(10, 245, 245), // The castle windows
				Color(248, 255, 255), // The subtitle inner color
				Color(157, 230, 252), // The subtitle outer color
			)
			Thread.sleep(100)
			testRendering(
				igState, 800, 500, "intro-cutscene2",
				cutsceneColors + TestTitleScreen.titleBarColors, titleScreenColors
			)

			// Skip the cutscene
			context.input.postEvent(pressKeyEvent(InputKey.Interact))

			val dragonLairColors = arrayOf(
				Color(13, 0, 22), // background color
				Color(77, 69, 95), // brick color
			)

			// Check fade-in
			repeat(2) {
				igState.update(context)
			}
			testRendering(
				igState, 1000, 800, "intro-dialogue0",
				emptyArray(), dragonLairColors + dialogueBoxColors + eButtonColors
			)

			// Skip the rest of the fade-in
			repeat(48) {
				igState.update(context)
			}

			testRendering(
				igState, 1000, 800, "intro-dialogue1",
				dragonLairColors, dialogueBoxColors + eButtonColors
			)

			// This should have no effect since the player is forced to walk
			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))

			val areaState = (igState.campaign.state as AreaState)
			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertTrue(actions.node is FixedActionNode)
			assertTrue((actions.node as FixedActionNode).action is ActionWalk)

			repeat(500) {
				igState.update(context)
			}

			val mardekTalk1 = (actions.node as FixedActionNode).action as ActionTalk
			assertEquals(ActionTargetPartyMember(0), mardekTalk1.speaker)
			assertEquals("norm", mardekTalk1.expression)
			assertEquals("Well Deugan, this is The Dragon's Lair.", mardekTalk1.text)
			assertEquals(actions.shownDialogueCharacters, mardekTalk1.text.length.toFloat())

			assertEquals(AreaPosition(5, 5), areaState.getPlayerPosition(0))
			assertEquals(Direction.Down, areaState.getPlayerDirection(0))
			assertEquals(AreaPosition(5, 6), areaState.getPlayerPosition(1))
			assertEquals(Direction.Up, areaState.getPlayerDirection(1))

			testRendering(
				igState, 1000, 800, "intro-dialogue2",
				dragonLairColors + dialogueBoxColors + eButtonColors, emptyArray()
			)

			// Go to next dialogue node
			context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			igState.update(context)

			val deuganTalk1 = (actions.node as FixedActionNode).action as ActionTalk
			assertEquals(ActionTargetPartyMember(1), deuganTalk1.speaker)
			assertEquals("grin", deuganTalk1.expression)
			assertEquals("Well Deugan, this is The Dragon's Lair.", mardekTalk1.text)
			assertTrue(
				actions.shownDialogueCharacters < 5f,
				"Expected ${actions.shownDialogueCharacters} to be small",
			)

			// Ideally, I would test that we render a different portrait, but I can't find reliable colors to test...
			testRendering(
				igState, 1000, 800, "intro-dialogue3",
				dragonLairColors + dialogueBoxColors, emptyArray(),
			)

			// Skip the rest of the dialogue
			context.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(500) {
				igState.update(context)
			}

			assertNull(areaState.suspension)
			assertEquals(AreaPosition(5, 5), areaState.getPlayerPosition(0))
			assertEquals(Direction.Down, areaState.getPlayerDirection(0))
			assertEquals(AreaPosition(5, 6), areaState.getPlayerPosition(1))
			assertEquals(Direction.Up, areaState.getPlayerDirection(1))
			testRendering(
				igState, 1000, 800, "intro-dialogue4",
				dragonLairColors, dialogueBoxColors + eButtonColors
			)

			// Test that we can walk away now that the dialogue is over
			context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(200) {
				igState.update(context)
			}

			assertEquals(AreaPosition(10, 5), areaState.getPlayerPosition(0))
		}
	}
}
