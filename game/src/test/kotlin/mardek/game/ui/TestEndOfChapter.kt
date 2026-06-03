package mardek.game.ui

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

object TestEndOfChapter {

	fun runTest(instance: TestingInstance) {
		instance.apply {
			val saves = SavesFolderManager(Files.createTempDirectory("").toFile())
			saves.root.deleteOnExit()

			val state = InGameState(simpleCampaignState(), "EndOfChapter1")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 10.milliseconds, saves
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Dropped Deugan home before after Rohoph entered Mardeks body"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_Mhouse2" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(0, 1)
			)

			// Skip the dialogues
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(1000) {
				state.update(updateContext)
			}
			sleep(3000)
			testRendering(
				state, 900, 700, "end-of-chapter-1",
				arrayOf(), arrayOf(),
			)
			repeat(1000) {
				state.update(updateContext)
			}
			sleep(2500)
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))

			val coreColors = arrayOf(
				Color(73, 59, 50), // Title bar
				Color(0, 0, 0, 0), // Transparent black background
				Color(131, 81, 38), // Title text color
				Color(238, 203, 127), // Info text color
				Color(248, 232, 194), // Upper button text color
				Color(255, 204, 153), // Button outline color
			)
			testRendering(
				state, 900, 700, "end-of-chapter-2",
				coreColors, arrayOf(),
			)

			val campaignActions = state.campaign.state as CampaignActionsState
			val endState = campaignActions.endOfChapterState!!

			updateContext.input.postEvent(MouseMoveEvent(
				endState.itemStorageButton!!.minX, endState.itemStorageButton!!.minY
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)

			val itemStorageColors = arrayOf(
				Color(73, 59, 50), // Title bar
				Color(22, 13, 13), // Upper bar
				Color(153, 153, 153), // Thrash icon
				Color(195, 157, 79), // Deugan hair
			)
			val inventoryColors = arrayOf(
				Color(101, 101, 50), // Tunic
				Color(104, 64, 28), // Stick
			)
			testRendering(
				state, 900, 700, "end-of-chapter-3",
				itemStorageColors, emptyArray(),
			)

			updateContext.input.postEvent(MouseMoveEvent(
				endState.itemStorage!!.renderedCharacters[0].region.maxX,
				endState.itemStorage!!.renderedCharacters[0].region.maxY,
			))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Click))
			state.update(updateContext)

			testRendering(
				state, 900, 700, "end-of-chapter-4",
				itemStorageColors + inventoryColors, arrayOf(),
			)

			assertEquals("Tunic", state.campaign.characterStates[childMardek]!!.equipment[
				childMardek.characterClass.equipmentSlots[3]
			]!!.displayName)

			val renderedBar = endState.itemStorage!!.renderedCharacterBar!!
			updateContext.input.postEvent(MouseMoveEvent(
				renderedBar.startX + 3 * renderedBar.slotSpacing, renderedBar.startY
			))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Click))
			state.update(updateContext)

			updateContext.input.postEvent(MouseMoveEvent(
				endState.itemStorage!!.renderedStorageInventory!!.startX,
				endState.itemStorage!!.renderedStorageInventory!!.startY,
			))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Click))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(updateContext)

			assertNull(state.campaign.characterStates[childMardek]!!.equipment[
				childMardek.characterClass.equipmentSlots[3]
			])
			assertEquals("Tunic", state.campaign.itemStorage[0]!!.item.displayName)

			// Close item storage
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)

			// Since the item storage button should still be selected, we can select the save button using the up key
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val saveColors = arrayOf(
				Color(73, 59, 50), // Title bar
				Color(22, 13, 13), // Upper bar
				Color(165, 204, 254), // Outline color
				Color(51, 51, 204), // Crystal pointer
			)
			testRendering(
				state, 900, 700, "end-of-chapter-5",
				saveColors, arrayOf(),
			)

			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNull(endState.itemStorage)
			assertNull(endState.saveSelectionState)

			assertArrayEquals(arrayOf("EndOfChapter1"), saves.root.list())
			assertEquals(1, saves.root.listFiles()!![0].list()!!.size)

			saves.root.deleteRecursively()
		}
	}
}
