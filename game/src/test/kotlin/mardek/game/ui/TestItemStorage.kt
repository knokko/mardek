package mardek.game.ui

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestItemStorage {

	private val baseColors = arrayOf(
		Color(22, 13, 13), // Upper bar color
		Color(131, 81, 38), // Upper text color
		Color(153, 153, 153), // Thrash color
		Color(255, 255, 0), // Gold color
		Color(208, 193, 142), // Slot border color
		Color(255, 204, 153), // Skin color of Mardek & Deugan
	)
	private val selectedSlotColors = arrayOf(
		Color(165, 205, 254), // Border color
	)
	private val tunicColors = arrayOf(
		Color(101, 101, 50), // Dark color
	)
	val huffPuffColors = arrayOf(
		Color(254, 95, 95), // Mastery text color
		Color(242, 135, 106), // Red rainbow color
		Color(128, 156, 184), // Blue rainbow color
	)

	private fun openItemStorage(instance: TestingInstance, state: InGameState, updateContext: GameStateUpdateContext) {
		instance.apply {
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)

			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				state.campaign.story, AreaPosition(10, 3)
			)

			// Interact with the save crystal
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Skip waiting for the save crystal dialogue
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(100) {
				state.update(updateContext)
			}
			assertNotNull(updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Scroll down to "Item storage..."
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			// Choose "Item storage..."
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
		}
	}

	fun testPutTunicInStorage(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			openItemStorage(instance, state, updateContext)

			testRendering(
				state, 900, 700, "item-storage1",
				baseColors,
				selectedSlotColors + tunicColors + huffPuffColors,
			)

			val actions = ((state.campaign.state as AreaState).suspension as AreaSuspensionActions).actions
			val interaction = actions.itemStorageInteraction!!
			val mardekSlotRegion = interaction.renderedCharacters[0].region

			// Open the inventory of Mardek
			updateContext.input.postEvent(MouseMoveEvent(mardekSlotRegion.minX, mardekSlotRegion.minY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			testRendering(
				state, 900, 700, "item-storage2",
				baseColors + selectedSlotColors + tunicColors,
				huffPuffColors,
			)

			// We should NOT be able to scroll to the next page, because the item storage is empty
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertNull(updateContext.soundQueue.take())
			assertEquals(0, interaction.storagePage)

			// Grab the tunic of Mardek
			val characterBar = interaction.renderedCharacterBar!!
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(MouseMoveEvent(
				characterBar.startX + 3 * characterBar.slotSpacing, characterBar.startY
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))

			val tunic = content.items.items.find { it.displayName == "Tunic" }!!
			assertSame(tunic, state.campaign.characterStates[childMardek]!!.equipment[childMardek.characterClass.equipmentSlots[3]])
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// And put it in the item storage
			val renderedStorage = interaction.renderedStorageInventory!!
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(MouseMoveEvent(
				renderedStorage.startX + 3 * renderedStorage.slotSize,
				renderedStorage.startY + renderedStorage.slotSize,
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.clickCancel, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 900, 700, "item-storage3",
				baseColors + selectedSlotColors + tunicColors,
				huffPuffColors,
			)

			// Scrolling to the right should be possible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.scroll1, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 900, 700, "item-storage4",
				baseColors + selectedSlotColors + tunicColors + huffPuffColors,
				emptyArray(),
			)

			// Scrolling down should go to the next item storage page, which no longer contains the tunic
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertEquals(1, interaction.storagePage)
			assertSame(content.audio.fixedEffects.ui.scroll2, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Unfortunately, this requires 2 frames
			testRendering(
				state, 900, 700, "item-storage5",
				baseColors, tunicColors + huffPuffColors,
			)
			testRendering(
				state, 900, 700, "item-storage6",
				baseColors + selectedSlotColors,
				tunicColors + huffPuffColors,
			)

			// Scrolling down *again* should not be possible, because we haven't put anything into the last page
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertNull(updateContext.soundQueue.take())
			assertEquals(1, interaction.storagePage)

			assertEquals(ItemStack(tunic, 1), state.campaign.itemStorage[19])
			assertNull(state.campaign.cursorItemStack)
			assertNull(state.campaign.characterStates[childMardek]!!.equipment[childMardek.characterClass.equipmentSlots[3]])
		}
	}

	fun testTakeTunicFromStorage(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			val tunic = content.items.items.find { it.displayName == "Tunic" }!!
			while (state.campaign.itemStorage.size < 20) state.campaign.itemStorage.add(null)
			state.campaign.itemStorage[19] = ItemStack(tunic, 1)

			openItemStorage(instance, state, updateContext)

			val forbiddenSlotColor = arrayOf(Color(255, 162, 162))
			val equalStatColor = arrayOf(Color(0, 220, 255))
			testRendering(
				state, 900, 700, "item-storage7",
				baseColors + tunicColors,
				selectedSlotColors + huffPuffColors + forbiddenSlotColor + equalStatColor,
			)

			val actions = ((state.campaign.state as AreaState).suspension as AreaSuspensionActions).actions
			val interaction = actions.itemStorageInteraction!!
			val deuganSlotRegion = interaction.renderedCharacters[1].region

			// Open the inventory of Deugan
			updateContext.input.postEvent(MouseMoveEvent(deuganSlotRegion.minX, deuganSlotRegion.minY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			testRendering(
				state, 900, 700, "item-storage8",
				baseColors + selectedSlotColors + tunicColors,
				huffPuffColors + forbiddenSlotColor + equalStatColor,
			)

			// Take the tunic from the item storage
			val renderedStorage = interaction.renderedStorageInventory!!
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(MouseMoveEvent(
				renderedStorage.startX + 3 * renderedStorage.slotSize,
				renderedStorage.startY + renderedStorage.slotSize,
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertTrue(state.campaign.itemStorage.all { it == null })
			assertEquals(ItemStack(tunic, 1), state.campaign.cursorItemStack)
			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			// And hover it above the helmet slot, which should show a red (forbidden) slot color
			val renderedBar = interaction.renderedCharacterBar!!
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(MouseMoveEvent(
				renderedBar.startX + 2 * renderedBar.slotSpacing, renderedBar.startY,
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.clickReject, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 900, 700, "item-storage9",
				baseColors + tunicColors + forbiddenSlotColor,
				huffPuffColors + selectedSlotColors + equalStatColor,
			)

			// And put it in the inventory of Deugan
			val renderedInventory = interaction.renderedCharacterInventory!!
			updateContext.input.postEvent(MouseMoveEvent(
				renderedInventory.startX + renderedInventory.slotSize,
				renderedInventory.startY + renderedInventory.slotSize,
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.clickCancel, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			assertNull(state.campaign.cursorItemStack)
			assertTrue(state.campaign.itemStorage.all { it == null })

			val deuganState = state.campaign.characterStates[childDeugan]!!
			assertEquals(ItemStack(tunic, 1), deuganState.inventory[9])

			// Try to take the weapon from Deugan, which is not allowed
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(MouseMoveEvent(renderedBar.startX, renderedBar.startY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.clickReject, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 900, 700, "item-storage10",
				baseColors + tunicColors + selectedSlotColors + equalStatColor,
				huffPuffColors + forbiddenSlotColor,
			)

			val bigStick = content.items.items.find { it.displayName == "Big Stick" }!!
			assertSame(bigStick, deuganState.equipment[childDeugan.characterClass.equipmentSlots[0]])
			assertNull(state.campaign.cursorItemStack)
		}
	}
}
