package mardek.game.area

import mardek.content.area.Direction
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
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionOpeningChest
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestChestLoot {

	fun testControlsAndRendering(instance: TestingInstance) {
		instance.apply {
			val potion = content.items.items.find { it.displayName == "Potion" }!!

			val campaign = simpleCampaignState()
			campaign.currentArea = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				AreaPosition(28, 6), skipFadeIn = true
			)

			val input = InputManager()
			val state = GameStateManager(
				input, InGameState(campaign, "test"),
				SavesFolderManager(),
			)
			val soundQueue = SoundQueue()
			val context = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, 100.milliseconds), ""
			)
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

			val openChest = (campaign.currentArea!!.suspension as AreaSuspensionOpeningChest).obtainedItem!!
			assertEquals(0, openChest.partyIndex)

			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			campaign.update(context)
			assertEquals(1, openChest.partyIndex)
			assertSame(content.audio.fixedEffects.ui.scroll1, soundQueue.take())
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
			assertSame(content.audio.fixedEffects.ui.scroll1, soundQueue.take())
			assertNull(soundQueue.take())

			// Luckily, Mardek has plenty of space
			val mardekState = campaign.characterStates[heroMardek]!!
			input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			assertEquals(0, mardekState.countItemOccurrences(potion))
			assertSame(openChest, (campaign.currentArea!!.suspension as AreaSuspensionOpeningChest).obtainedItem)
			assertEquals(0, campaign.openedChests.size)
			campaign.update(context)
			assertSame(content.audio.fixedEffects.ui.clickCancel, soundQueue.take())
			assertNull(soundQueue.take())
			assertEquals(1, mardekState.countItemOccurrences(potion))

			assertEquals(1, campaign.openedChests.size)
			assertNull(campaign.currentArea!!.suspension)
			testRendering(
				state, 900, 450, "chest-after-close",
				areaColors + partyColors, lootColors
			)

			// Check that the chest can't be opened again
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertFalse(campaign.currentArea!!.suspension is AreaSuspensionOpeningChest)
			assertNull(soundQueue.take())
		}
	}

	fun testChestWithGold(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.currentArea = AreaState(
				content.areas.areas.find { it.properties.rawName == "lakequr_cave2" }!!,
				AreaPosition(5, 48), Direction.Down, skipFadeIn = true
			)

			val input = InputManager()
			val state = GameStateManager(
				input, InGameState(campaign, "test"),
				SavesFolderManager(),
			)
			val soundQueue = SoundQueue()
			val context = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, 100.milliseconds), ""
			)

			val partyColors = arrayOf(
				Color(32, 75, 101), // Mardek armor
				Color(7, 49, 16), // Deugan robe
			)
			val areaColors = arrayOf(
				Color(1, 2, 8), // Cave wall color
				Color(46, 104, 117), // Cave floor color
				Color(7, 17, 16), // Chest color
			)
			val goldColors = arrayOf(
				Color(255, 255, 0), // Gold icon
				Color(187, 149, 38), // Gold text
			)
			testRendering(
				state, 900, 450, "chest-gold-before-open",
				areaColors + partyColors, goldColors
			)

			input.postEvent(pressKeyEvent(InputKey.Interact))
			assertEquals(123, campaign.gold)
			campaign.update(context)
			assertEquals(123 + 56, campaign.gold)
			assertSame(content.audio.fixedEffects.openChest, soundQueue.take())
			assertNull(soundQueue.take())
			assertNull(campaign.currentArea!!.suspension)

			testRendering(
				state, 900, 450, "chest-gold-after-open",
				goldColors + partyColors + areaColors, emptyArray(),
			)

			// Test that the chest cannot be opened again
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(123 + 56, campaign.gold)
			assertNull(soundQueue.take())
			assertNull(campaign.currentArea!!.suspension)
		}
	}
}
