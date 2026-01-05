package mardek.game.ui

import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionShowChapterName
import mardek.content.action.FixedActionNode
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.TextTypeEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.saves.SaveFile
import mardek.state.title.StartNewGameState
import mardek.state.title.TitleScreenState
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

object TestTitleScreen {

	internal val titleBarColors = arrayOf(
		Color(73, 59, 50),
		Color(132, 105, 83),
	)

	internal val baseColors = arrayOf(
		Color(190, 144, 95), // Title outer outline
		Color(69, 50, 34), // Title inner outline
		Color(242, 183, 113), // Subtitle upper color
		Color(184, 130, 61), // Subtitle lower color
		Color(255, 204, 153), // Button border color
	)

	private val selectedButtonColors = arrayOf(
		Color(152, 190, 222),
		Color(28, 69, 122),
	)

	private val disabledButtonColors = arrayOf(
		Color(102, 73, 49),
	)

	private val textInputColors = arrayOf(
		Color(61, 34, 22),
	)

	fun testNewGame(instance: TestingInstance) {
		instance.apply {
			val input = InputManager()
			val soundQueue = SoundQueue()
			val saves = dummySaveManager()

			val saveFile = createDummySave(saves, "existing")

			val state = TitleScreenState()
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(-1, state.selectedButton)
			testRendering(
				state, 800, 450, "new-game0",
				baseColors + titleBarColors,
				selectedButtonColors + textInputColors + disabledButtonColors,
			)

			// 'Hover' over "New Game"
			input.postEvent(pressKeyEvent(InputKey.CheatScrollDown))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(0, state.selectedButton)
			assertNull(state.newCampaignName)
			assertFalse(state.isCampaignNameValid)
			testRendering(
				state, 800, 450, "new-game1",
				baseColors + selectedButtonColors + titleBarColors,
				textInputColors + disabledButtonColors,
			)

			// 'Click' on "New Game"
			input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(0, state.selectedButton)
			assertEquals("", state.newCampaignName)
			assertFalse(state.isCampaignNameValid)
			testRendering(
				state, 800, 450, "new-game2",
				baseColors + selectedButtonColors + textInputColors + disabledButtonColors, emptyArray()
			)

			// Change the campaign name from "" to "."
			input.postEvent(TextTypeEvent("."))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(".", state.newCampaignName)
			assertFalse(state.isCampaignNameValid)

			// Clicking on "BEGIN" should have no effect since the campaign name "." is invalid
			state.selectedButton = 4
			input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))

			// Change the campaign name from "." to ".k", which should be a valid campaign name on any desktop OS
			input.postEvent(TextTypeEvent("k"))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(".k", state.newCampaignName)
			assertTrue(state.isCampaignNameValid)
			testRendering(
				state, 800, 450, "new-game3",
				baseColors + selectedButtonColors + textInputColors, disabledButtonColors
			)

			// Pressing the Interact key should NOT start the campaign, since that would auto-start the campaign when
			// players try to put an E in the campaign name
			input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertEquals(".k", state.newCampaignName)
			assertTrue(state.isCampaignNameValid)

			// Click on Load Game, which should reset the campaign name
			state.selectedButton = 1
			input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			assertNull(state.newCampaignName)
			assertNull(state.saveSelection)

			// Ok, let's assume the content has finally finished loading
			val context = GameStateUpdateContext(content, input, soundQueue, 100.milliseconds, saves)
			assertSame(state, state.update(context))
			assertNull(state.newCampaignName)
			val saveSelection = state.saveSelection!!
			assertArrayEquals(arrayOf("existing"), saveSelection.selectableCampaigns)

			// Cancel "Load Game"
			input.postEvent(pressKeyEvent(InputKey.Cancel))
			assertSame(state, state.update(context))
			assertNull(state.newCampaignName)
			assertNull(state.saveSelection)
			testRendering(
				state, 800, 450, "new-game4",
				baseColors + selectedButtonColors + titleBarColors,
				textInputColors + disabledButtonColors,
			)

			// Click on "New Game" again
			state.selectedButton = 0
			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			assertSame(state, state.update(context))
			assertEquals("", state.newCampaignName)
			assertFalse(state.isCampaignNameValid)
			testRendering(
				state, 800, 450, "new-game5",
				baseColors + selectedButtonColors + textInputColors + disabledButtonColors, emptyArray()
			)

			// Type "lets go" in the text field
			input.postEvent(TextTypeEvent("lets go"))
			assertSame(state, state.update(context))
			assertEquals("lets go", state.newCampaignName)
			assertTrue(state.isCampaignNameValid)
			testRendering(
				state, 800, 450, "new-game6",
				baseColors + selectedButtonColors + textInputColors, disabledButtonColors
			)

			state.selectedButton = 4
			val beforeClickTime = System.nanoTime()
			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))

			val startingState = state.update(context) as StartNewGameState
			assertSame(state, startingState.titleState)
			assertTrue(startingState.beginButtonClickTime > beforeClickTime)

			// Skip title screen fade
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			val newState = startingState.update(context) as InGameState

			// Skip chapter name
			assertInstanceOf<ActionShowChapterName>((newState.campaign.actions!!.node as FixedActionNode).action)
			context.input.postEvent(pressKeyEvent(InputKey.Escape))
			newState.update(context)

			// Skip intro cutscene
			assertInstanceOf<ActionPlayCutscene>((newState.campaign.actions!!.node as FixedActionNode).action)
			context.input.postEvent(pressKeyEvent(InputKey.Cancel))
			newState.update(context)

			// Finally go in-game
			assertNull(newState.campaign.actions)
			assertEquals("lets go", newState.campaignName)
			assertSame(heroMardek, newState.campaign.party[0])

			// Skip fade-in effect
			repeat(5) {
				newState.update(context)
			}

			val expectedAreaColors = arrayOf(
				Color(59, 53, 68), // color between floor tiles
			)
			testRendering(
				newState, 800, 450, "new-game7",
				expectedAreaColors + titleBarColors,
				textInputColors + disabledButtonColors,
			)

			assertTrue(saveFile.delete())
			assertTrue(saveFile.parentFile.delete())
		}
	}

	fun testLoadGameWithoutCampaignsEdgeCase1(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			createDummySave(saves, "test")
			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds, saves
			)

			val state = TitleScreenState()
			state.selectedButton = 1
			assertSame(state, state.update(context))

			// Delete the save file, AFTER updating the state for the first time
			assertTrue(File("${saves.root}/test").deleteRecursively())

			// Click on "Load Game", which should open the outdated save,
			// and instantly notice that it is no longer valid
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(state.saveSelection)
			assertEquals(0, state.availableCampaigns!!.size)
		}
	}

	fun testLoadGameWithoutCampaignsEdgeCase2(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			createDummySave(saves, "test")
			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds, saves
			)

			val state = TitleScreenState()
			state.selectedButton = 1
			assertSame(state, state.update(context))

			// Delete the save file, AFTER updating the state for the first time
			assertTrue(File("${saves.root}/test").deleteRecursively())

			// Click on "Load Game", which should open the outdated save,
			// and instantly notice that it is no longer valid
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(state.saveSelection)
			assertEquals(0, state.availableCampaigns!!.size)
		}
	}

	fun testLoadGameMissingCampaignEdgeCase(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			createDummySave(saves, "test1")
			createDummySave(saves, "test2")
			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds, saves
			)

			val state = TitleScreenState()
			state.selectedButton = 1
			assertSame(state, state.update(context))

			// Delete the save file of test1, AFTER updating the state for the first time
			assertTrue(File("${saves.root}/test1").deleteRecursively())

			// Click on "Load Game", which should open the outdated save,
			// and instantly notice that it is no longer valid
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(state.saveSelection)
			assertArrayEquals(arrayOf("test2"), state.availableCampaigns)

			// Retry: only the valid test2 should be available now
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertInstanceOf<InGameState>(state.update(context))
		}
	}

	fun testLoadGameMissingSaveEdgeCase(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val save1 = createDummySave(saves, "test")
			val info1 = SaveFile.scan(save1)!!
			val save2 = File("${save1.parentFile}/crystal-${info1.timestamp - 1}.bits")
			Files.copy(save1.toPath(), save2.toPath())

			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds, saves
			)

			val state = TitleScreenState()
			state.selectedButton = 1
			assertSame(state, state.update(context))

			// Click on "Load Game"
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertNotNull(state.saveSelection)

			// Delete the first save file, AFTER updating the save selection state for the first time
			assertTrue(save1.delete())

			// Attempt to load the first save, which we just deleted
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(state.saveSelection)
			assertArrayEquals(arrayOf("test"), state.availableCampaigns)

			// Try again: this time, the deleted save should no longer be selectable
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertInstanceOf<InGameState>(state.update(context))
		}
	}

	fun testLoadGameHappyFlow(instance: TestingInstance) {
		instance.apply {
			val input = InputManager()
			val soundQueue = SoundQueue()
			val saves = dummySaveManager()

			val emptyDirectory = File("${saves.root}/A_empty")
			assertTrue(emptyDirectory.mkdir())
			emptyDirectory.deleteOnExit()

			val singleFile = createDummySave(saves, "B_single")
			val singleDirectory = singleFile.parentFile
			val manyFile = createDummySave(saves, "C_many")
			val manyDirectory = manyFile.parentFile

			run {
				for (copyIndex in 0 until 100) {
					val destination = File("${manyDirectory}/crystal-${copyIndex}.bits")

					if (copyIndex == 50) {
						val specialCampaign = simpleCampaignState()
						specialCampaign.characterStates[heroMardek]!!.currentLevel = 123
						specialCampaign.characterStates[heroDeugan]!!.currentLevel = 12
						saves.writeSaveTo(content, specialCampaign, destination)
					} else {
						Files.copy(manyFile.toPath(), destination.toPath())
					}
					destination.deleteOnExit()
				}
			}

			val state = TitleScreenState()
			state.selectedButton = 1
			assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
			testRendering(
				state, 800, 450, "load-game0",
				baseColors + selectedButtonColors, textInputColors + disabledButtonColors
			)

			val sounds = content.audio.fixedEffects.ui
			input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(5) {
				assertSame(state, state.updateBeforeContent(input, soundQueue, saves))
				// saveSelection should become non-null once we call the 'real' update instead of updateBeforeContent
				assertNull(state.saveSelection)
			}

			val loadGameColors = arrayOf(
				Color(131, 81, 13), // "Load" text in upper bar
				Color(22, 13, 13), // "Upper bar"
				Color(73, 59, 50), // Title bar
				Color(132, 105, 83), // Title bar
				Color(116, 101, 88), // Mardek armor
				Color(70, 117, 33), // Deugan armor
				Color(51, 153, 204), // Crystal pointer
				Color(51, 51, 204), // Crystal pointer
				Color(147, 108, 57), // Clock color
				Color(238, 203, 127), // Text color
				Color(165, 204, 254), // Selected border color
			)
			val defaultBorderColors = arrayOf(Color(208, 193, 142))
			val grayBorderColors = arrayOf(Color(180, 170, 134))

			val updateContext = GameStateUpdateContext(content, input, soundQueue, 1.milliseconds, saves)
			assertSame(state, state.update(updateContext))
			var saveSelection = state.saveSelection!!
			assertEquals(0, saveSelection.selectedFileIndex)
			assertEquals("B_single", saveSelection.getSelectedCampaign())
			assertEquals(0, saveSelection.selectedFileIndex)
			assertArrayEquals(arrayOf("B_single", "C_many"), saveSelection.selectableCampaigns)
			assertEquals(1, saveSelection.selectableFiles.size)
			testRendering(
				state, 800, 450, "load-game1",
				loadGameColors + grayBorderColors, selectedButtonColors
			)

			// Let's cancel and go back
			input.postEvent(pressKeyEvent(InputKey.Cancel))
			assertSame(state, state.update(updateContext))
			assertSame(sounds.clickCancel, soundQueue.take())
			assertNull(state.saveSelection)

			input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.update(updateContext))
			assertNull(soundQueue.take())
			saveSelection = state.saveSelection!!
			assertEquals(0, saveSelection.selectedFileIndex)
			assertEquals("B_single", saveSelection.getSelectedCampaign())

			// Pressing Down should not have any effect since there is only 1 save in the B_single campaign
			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			assertSame(state, state.update(updateContext))
			assertEquals(0, saveSelection.selectedFileIndex)
			assertNull(soundQueue.take())

			// Pressing Left should not have any effect either
			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			assertSame(state, state.update(updateContext))
			assertEquals("B_single", saveSelection.getSelectedCampaign())

			// Neither should pressing Up
			input.postEvent(pressKeyEvent(InputKey.MoveUp))
			assertSame(state, state.update(updateContext))
			assertEquals(0, saveSelection.selectedFileIndex)
			assertNull(soundQueue.take())

			// Pressing Right on the other hand... should switch the selected campaign to "C_many"
			input.postEvent(pressKeyEvent(InputKey.MoveRight))
			assertSame(state, state.update(updateContext))
			assertEquals("C_many", saveSelection.getSelectedCampaign())
			assertEquals(0, saveSelection.selectedFileIndex)
			assertSame(sounds.scroll2, soundQueue.take())

			// Since C_many has multiple saves, we can scroll down
			repeat(5) {
				input.postEvent(pressKeyEvent(InputKey.MoveDown))
			}
			assertSame(state, state.update(updateContext))
			repeat(5) {
				assertSame(sounds.scroll1, soundQueue.take())
			}
			assertEquals("C_many", saveSelection.getSelectedCampaign())
			assertEquals(5, saveSelection.selectedFileIndex)

			// We can also scroll back up
			for (counter in 0 until 20) {
				input.postEvent(pressKeyEvent(InputKey.MoveUp))
				assertSame(state, state.update(updateContext))
				if (counter < 5) assertSame(sounds.scroll1, soundQueue.take())
				else assertNull(soundQueue.take())
			}
			assertEquals(0, saveSelection.selectedFileIndex)

			// Now let's scroll to the end
			for (counter in 0 until 123) {
				input.postEvent(pressKeyEvent(InputKey.MoveDown))
				assertSame(state, state.update(updateContext))
				if (counter < 100) assertSame(sounds.scroll1, soundQueue.take())
				else assertNull(soundQueue.take())
			}
			assertEquals(100, saveSelection.selectedFileIndex)

			// Let's scroll to the special save 50
			repeat(50) {
				input.postEvent(pressKeyEvent(InputKey.MoveUp))
				assertSame(state, state.update(updateContext))
				assertSame(sounds.scroll1, soundQueue.take())
			}
			assertEquals(50, saveSelection.selectedFileIndex)

			testRendering(
				state, 800, 450, "load-game2",
				loadGameColors + defaultBorderColors,
				selectedButtonColors + grayBorderColors,
			)

			input.postEvent(pressKeyEvent(InputKey.Interact))
			val nextState = state.update(updateContext) as InGameState
			assertEquals("C_many", nextState.campaignName)
			assertEquals(123, nextState.campaign.characterStates[heroMardek]!!.currentLevel)

			emptyDirectory.delete()
			singleDirectory.deleteRecursively()
			manyDirectory.deleteRecursively()
		}
	}
}
