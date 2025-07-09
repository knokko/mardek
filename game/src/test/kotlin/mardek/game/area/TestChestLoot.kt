package mardek.game.area

import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestChestLoot {

	fun testControlsAndRendering(instance: TestingInstance) {
		instance.apply {
			val potion = content.items.items.find { it.flashName == "Potion" }!!

			val campaign = simpleCampaignState()
			campaign.currentArea = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				AreaPosition(28, 6)
			)

			val input = InputManager()
			val state = GameStateManager(input, InGameState(campaign))
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 100.milliseconds)
			input.postEvent(pressKeyEvent(InputKey.MoveRight))
			campaign.update(context)
			assertNull(soundQueue.take())

			val partyColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(70, 117, 33), // Deugan robe
			)
			val areaColors = arrayOf(
				Color(23, 66, 40), // Tree/grass color
				Color(102, 51, 153), // Mushroom color
				Color(88, 66, 50), // Chest color
			)
			val lootColors = arrayOf(
				Color(203, 153, 0), // TREASURE text color
				Color(238, 203, 127), // Other text color
				Color(0, 90, 170), // Potion color
				Color(99, 128, 177), // Party highlight color
				Color(81, 113, 217), // Inventory grid consumable color
			)
			testRendering(
				state, 900, 450, "chest-before-open",
				areaColors + partyColors, lootColors
			)

			input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertSame(content.audio.fixedEffects.openChest, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				state, 900, 450, "chest-after-open",
				lootColors + partyColors, areaColors
			)

			val openChest = campaign.currentArea!!.obtainedItemStack!!
			assertEquals(0, openChest.partyIndex)

			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			campaign.update(context)
			assertEquals(1, openChest.partyIndex)
			assertSame(content.audio.fixedEffects.ui.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			campaign.update(context)
			assertEquals(1, openChest.partyIndex)
			assertNull(soundQueue.take())

			val deuganState = campaign.characterStates[heroDeugan]!!
			for (index in deuganState.inventory.indices) {
				deuganState.inventory[index] = ItemStack(Item(), 1)
			}

			// Whoops, Deugan does not have any inventory space
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertSame(content.audio.fixedEffects.ui.clickReject, soundQueue.take())
			assertNull(soundQueue.take())

			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveRight))
			campaign.update(context)
			assertEquals(0, openChest.partyIndex)
			assertSame(content.audio.fixedEffects.ui.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			// Luckily, Mardek has plenty of space
			val mardekState = campaign.characterStates[heroMardek]!!
			input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			assertEquals(0, mardekState.countItemOccurrences(potion))
			assertSame(openChest, campaign.currentArea!!.obtainedItemStack)
			assertEquals(0, campaign.openedChests.size)
			campaign.update(context)
			assertSame(content.audio.fixedEffects.ui.clickCancel, soundQueue.take())
			assertNull(soundQueue.take())
			assertEquals(1, mardekState.countItemOccurrences(potion))

			assertEquals(1, campaign.openedChests.size)
			assertNull(campaign.currentArea!!.obtainedItemStack)
			testRendering(
				state, 900, 450, "chest-after-close",
				areaColors + partyColors, lootColors
			)

			// Check that the chest can't be opened again
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertNull(campaign.currentArea!!.obtainedItemStack)
			assertNull(soundQueue.take())
		}
	}
}
