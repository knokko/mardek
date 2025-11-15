package mardek.game.ui

import mardek.content.action.ActionShowChapterName
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.TextTypeEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.title.StartNewGameState
import mardek.state.title.TitleScreenState
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
		}
	}
}
