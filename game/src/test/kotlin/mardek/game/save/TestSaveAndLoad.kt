package mardek.game.save

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.title.TitleScreenState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestSaveAndLoad {

	fun testOverwriteVanishedSave(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val oldSave = createDummySave(saves, "test-save-and-load")

			val areaState = AreaState(dragonLairEntry, AreaPosition(3, 4))
			val state = InGameState(CampaignState(
				currentArea = areaState,
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 21987,
			), "test-save-and-load")
			state.campaign.characterStates[heroMardek]!!.currentLevel = 5
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 6

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds, saves)

			// Interact with the crystal, and wait until the first dialogue message is fully rendered
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(10_000) {
				assertSame(state, state.update(context))
			}

			// Move on to the choice between "Save..." and "Cancel..."
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Choose save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			val actions = state.campaign.currentArea!!.actions!!

			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			assertSame(state, state.update(context))
			assertTrue(oldSave.delete())
			assertEquals(1, actions.saveSelectionState!!.selectableFiles.size)

			// Empty the sound queue
			while (true) {
				if (context.soundQueue.take() == null) break
			}

			// Try to overwrite old save, which fails because I deleted it
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())

			// The system should notice that the file no longer exists
			assertSame(state, state.update(context))
			val saveSelection = actions.saveSelectionState!!
			assertEquals(0, saveSelection.selectableFiles.size)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(state.campaign.currentArea!!.actions)
		}
	}

	fun testHappyFlow(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val areaState = AreaState(dragonLairEntry, AreaPosition(3, 4))
			var state: GameState = InGameState(CampaignState(
				currentArea = areaState,
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 21987,
			), "test-save-and-load")
			(state as InGameState).campaign.characterStates[heroMardek]!!.currentLevel = 5
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 6

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds, saves)

			// Interact with the crystal, and wait until the first dialogue message is fully rendered
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(10_000) {
				assertSame(state, state.update(context))
			}

			// Move on to the choice between "Save..." and "Cancel..."
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Choose save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Create new save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			assertNull(state.campaign.currentArea!!.actions)

			// Go to the title screen
			state = TitleScreenState()

			// Click on the "Load Game" button
			state.selectedButton = 1
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.update(context))
			val saveSelection = state.saveSelection!!
			assertEquals("test-save-and-load", saveSelection.getSelectedCampaign())
			assertEquals(1, saveSelection.selectableFiles.size)

			// Choose the first best save
			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state = state.update(context)
			assertEquals(21987, (state as InGameState).campaign.gold)
			assertEquals(5, state.campaign.characterStates[heroMardek]!!.currentLevel)
			assertEquals(6, state.campaign.characterStates[heroDeugan]!!.currentLevel)
			state.update(context)
			assertNull(state.campaign.currentArea!!.actions)

			val dragonLairColors = arrayOf(
				Color(13, 0, 22), // background color
				Color(77, 69, 95), // brick color
				Color(96, 199, 242), // crystal ring color
				Color(186, 255, 255), // crystal outline color
			)

			testRendering(
				state, 1000, 800, "save-and-load",
				dragonLairColors, emptyArray(),
			)
		}
	}
}
