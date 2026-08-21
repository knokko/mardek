package mardek.game.ui

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.state.GameState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestExitModal {

	fun runTest(instance: TestingInstance) {
		instance.apply {
			var state: GameState = InGameState(simpleCampaignState(), "exit")
			val areaState = (state as InGameState).campaign.state as AreaState
			val updateContext = createUpdateContext(10.milliseconds)

			state.update(updateContext)
			assertEquals(AreaPosition(10, 10), areaState.getPlayerPosition(0))

			val areaColors = arrayOf(
				Color(77, 69, 95), // Floor color
				Color(46, 22, 46), // Brazier color
				Color(102, 50, 0), // Mardek hair color
			)
			val modalColors = arrayOf(
				Color(238, 203, 127), // Base text color
				Color(255, 255, 152), // Bold text color
			)
			testRendering(
				state, 800, 600, "confirm-exit0",
				areaColors, modalColors,
			)

			// Open exit confirmation modal, and try to walk to the left (the latter should fail)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Escape))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			repeat(5) {
				state.update(updateContext)
			}
			testRendering(
				state, 800, 600, "confirm-exit1",
				emptyArray(), areaColors + modalColors
			)

			// Pressing Escape *again* has no effect until the modal is fully shown
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Escape))

			repeat(100) {
				state.update(updateContext)
			}

			// Since the player cannot move while the modal is shown, the position remains (10, 10)
			assertEquals(AreaPosition(10, 10), areaState.getPlayerPosition(0))
			testRendering(
				state, 800, 600, "confirm-exit2",
				modalColors, areaColors
			)

			// Cancel the exit modal
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Escape))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(10) {
				state.update(updateContext)
			}
			testRendering(
				state, 800, 600, "confirm-exit3",
				emptyArray(), areaColors + modalColors
			)

			repeat(90) {
				state.update(updateContext)
			}

			// Since the player was still holding the Left key, the player should start walking left,
			// once the modal is closed
			assertEquals(AreaPosition(7, 10), areaState.getPlayerPosition(0))
			testRendering(
				state, 800, 600, "confirm-exit4",
				areaColors, modalColors
			)

			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			repeat(50) {
				state.update(updateContext)
			}

			assertEquals(AreaPosition(6, 10), areaState.getPlayerPosition(0))

			// Open the modal again
			updateContext.input.postEvent(pressKeyEvent(InputKey.Escape))
			repeat(15) {
				state.update(updateContext)
			}
			repeat(10) {
				state.update(updateContext)
			}
			testRendering(
				state, 800, 600, "confirm-exit5",
				modalColors, areaColors
			)

			// Confirm this time, and render exit modal fade-out
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Escape))
			repeat(20) {
				state.update(updateContext)
			}
			testRendering(
				state, 800, 600, "confirm-exit6",
				emptyArray(), areaColors + modalColors
			)

			// Await title screen fade-in
			repeat(50) {
				state = state.update(updateContext)
			}
			sleep(550)
			state.update(updateContext)

			val titleScreenColors = arrayOf(
				Color(255, 204, 153), // Button outline color
				Color(241, 182, 113), // Subtitle color
			)
			testRendering(
				state, 800, 600, "confirm-exit6",
				titleScreenColors, emptyArray(),
			)
		}
	}
}
