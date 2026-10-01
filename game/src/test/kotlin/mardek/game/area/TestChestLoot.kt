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
import mardek.state.GameStateManager
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
			campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				campaign.story, campaign.expressionContext(),
				AreaPosition(28, 6), skipFadeIn = true,
			)

			val context = CampaignState.UpdateContext(
				createUpdateContext(10.milliseconds), ""
			)
			val state = GameStateManager(
				context.input, InGameState(campaign, "test"),
				SavesFolderManager(),
			)
			context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.currentState.update(context)
			assertNull(context.soundQueue.take())

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
				Color(165, 205, 254), // Party highlight color
				Color(81, 113, 217), // Inventory grid consumable color
				Color(167, 161, 141), // Light 'E' button color
				Color(145, 137, 112), // Dark 'E' button color
			)
			testRendering(
				state, 900, 450, "chest-before-open",
				areaColors + partyColors, lootColors
			)

			context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.currentState.update(context)
			assertSame(content.audio.fixedEffects.openChest, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Rendering during fade-in
			repeat(5) {
				state.currentState.update(context)
			}
			testRendering(
				state, 900, 450, "chest-during-open",
				emptyArray(), areaColors + partyColors
			)

			// Rendering after fade-in is finished
			repeat(20) {
				state.currentState.update(context)
			}
			testRendering(
				state, 900, 450, "chest-after-open",
				lootColors + partyColors, areaColors
			)

			val openChest = ((campaign.state as AreaState).suspension as AreaSuspensionOpeningChest).obtainedItem!!
			assertEquals(0, openChest.partyIndex)

			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.currentState.update(context)
			assertEquals(1, openChest.partyIndex)
			assertSame(content.audio.fixedEffects.ui.scroll1, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			state.currentState.update(context)
			assertEquals(1, openChest.partyIndex)
			assertNull(context.soundQueue.take())

			val deuganState = campaign.characterStates[heroDeugan]!!
			for (index in deuganState.inventory.indices) {
				deuganState.inventory[index] = ItemStack(Item(), 1)
			}

			// Whoops, Deugan does not have any inventory space
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.currentState.update(context)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.currentState.update(context)
			assertEquals(0, openChest.partyIndex)
			assertSame(content.audio.fixedEffects.ui.scroll1, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Luckily, Mardek has plenty of space
			val mardekState = campaign.characterStates[heroMardek]!!
			context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertEquals(0, mardekState.countItemOccurrences(potion))
			assertSame(openChest, ((campaign.state as AreaState).suspension as AreaSuspensionOpeningChest).obtainedItem)
			assertEquals(0, campaign.openedChests.size)
			state.currentState.update(context)
			assertSame(content.audio.fixedEffects.ui.clickCancel, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(1, mardekState.countItemOccurrences(potion))

			// Rendering during fade-out
			repeat(10) {
				state.currentState.update(context)
			}
			testRendering(
				state, 900, 450, "chest-during-close",
				emptyArray(), areaColors + partyColors
			)

			// Await the fade-out
			repeat(15) {
				state.currentState.update(context)
			}

			assertEquals(1, campaign.openedChests.size)
			assertNull((campaign.state as AreaState).suspension)
			testRendering(
				state, 900, 450, "chest-after-close",
				areaColors + partyColors, lootColors
			)

			// Check that the chest can't be opened again
			context.input.postEvent(repeatKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertFalse((campaign.state as AreaState).suspension is AreaSuspensionOpeningChest)
			assertNull(context.soundQueue.take())
		}
	}

	fun testChestWithGold(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "lakequr_cave2" }!!,
				campaign.story, campaign.expressionContext(),
				AreaPosition(5, 48), Direction.Down, skipFadeIn = true
			)


			val context = CampaignState.UpdateContext(
				createUpdateContext(100.milliseconds), ""
			)
			val state = GameStateManager(
				context.input, InGameState(campaign, "test"),
				SavesFolderManager(),
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
				Color(204, 153, 0), // Gold icon
				Color(255, 204, 50), // Gold text
			)
			testRendering(
				state, 1200, 800, "chest-gold-before-open",
				areaColors + partyColors, goldColors
			)

			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertEquals(123, campaign.gold)
			campaign.statistics.goldEarned = 50
			campaign.update(context)
			assertEquals(123 + 56, campaign.gold)
			assertEquals(50 + 56, campaign.statistics.goldEarned)
			assertSame(content.audio.fixedEffects.openChest, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertNull((campaign.state as AreaState).suspension)

			testRendering(
				state, 1200, 800, "chest-gold-after-open",
				goldColors + partyColors + areaColors, emptyArray(),
			)

			// Test that the chest cannot be opened again
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(123 + 56, campaign.gold)
			assertNull(context.soundQueue.take())
			assertNull((campaign.state as AreaState).suspension)
		}
	}
}
