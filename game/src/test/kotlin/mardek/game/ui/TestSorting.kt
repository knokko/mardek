package mardek.game.ui

import mardek.content.area.Direction
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.menu.inventory.InventoryTab
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.time.Duration.Companion.milliseconds

object TestSorting {

	fun testInventoryInMenu(instance: TestingInstance) {
		instance.apply {
			val potion = content.items.items.find { it.displayName == "Potion" }!!
			val ether = content.items.items.find { it.displayName == "Ether" }!!
			val keyBlade = content.items.items.find { it.displayName == "Keyblade" }!!
			val ring = content.items.items.find { it.displayName == "RingOfAGL" }!!

			val state = InGameState(simpleCampaignState(), "")
			val context = createUpdateContext(10.milliseconds)

			// Toggle in-game menu
			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			repeat(100) {
				state.update(context)
			}

			// Scroll to inventory
			repeat(2) {
				context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(context)
			}

			// Open inventory
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(context)

			// Switch from Mardek inventory to Deugan inventory
			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(context)

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.inventory[5] = ItemStack(ether, 1)

			val deuganState = state.campaign.characterStates[heroDeugan]!!
			deuganState.inventory[1] = ItemStack(ether, 3)
			deuganState.inventory[2] = ItemStack(ring, 1)
			deuganState.inventory[5] = ItemStack(ether, 1)
			deuganState.inventory[8] = ItemStack(potion, 4)
			deuganState.inventory[9] = ItemStack(keyBlade, 2)
			deuganState.inventory[10] = ItemStack(ring, 1)

			testRendering(
				state, 400, 300, "sort-inventory-menu",
				emptyArray(), emptyArray(),
			)

			val tab = state.menu.currentTab as InventoryTab
			val sortRegion = tab.sortRegion!!

			// Click on the sort button
			context.input.postEvent(MouseMoveEvent(sortRegion.minX, sortRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)

			val expectedInventory = Array<ItemStack?>(64) { null }
			expectedInventory[0] = ItemStack(keyBlade, 2)
			expectedInventory[1] = ItemStack(ring, 2)
			expectedInventory[2] = ItemStack(potion, 4)
			expectedInventory[3] = ItemStack(ether, 4)
			expectedInventory[4] = ItemStack(elixir, 1)

			assertArrayEquals(expectedInventory, deuganState.inventory)

			val expectedMardekInventory = Array<ItemStack?>(64) { null }
			expectedMardekInventory[5] = ItemStack(ether, 1)
			assertArrayEquals(expectedMardekInventory, mardekState.inventory)

			// Clicking *again* shouldn't have any effect
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)
			assertArrayEquals(expectedMardekInventory, mardekState.inventory)
			assertArrayEquals(expectedInventory, deuganState.inventory)
		}
	}

	fun testInventoryInShop(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = createUpdateContext(100.milliseconds)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_shop_I" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(2, 3), Direction.Up,
			)
			state.campaign.state = areaState

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Await fade-in
			repeat(3) {
				state.update(updateContext)
			}

			// Switch to the inventory of Deugan
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			val longSword = content.items.items.find { it.displayName == "Longsword" }!!
			val ironSword = content.items.items.find { it.displayName == "Iron Sword" }!!
			val wingShield = content.items.items.find { it.displayName == "Wing Shield" }!!
			val potion = content.items.items.find { it.displayName == "Potion" }!!

			val mardekState = state.campaign.characterStates[childMardek]!!
			mardekState.inventory[5] = ItemStack(wingShield, 1)

			val deuganState = state.campaign.characterStates[childDeugan]!!
			deuganState.inventory[1] = ItemStack(ironSword, 3)
			deuganState.inventory[2] = ItemStack(wingShield, 1)
			deuganState.inventory[5] = ItemStack(longSword, 1)
			deuganState.inventory[8] = ItemStack(potion, 4)
			deuganState.inventory[9] = ItemStack(ironSword, 2)
			deuganState.inventory[10] = ItemStack(potion, 1)

			testRendering(
				state, 800, 600, "sort-inventory-shop",
				emptyArray(), emptyArray(),
			)

			val interaction = ((state.campaign.state as AreaState).suspension as AreaSuspensionActions).actions.shopInteraction!!
			val sortRegion = interaction.sortRegion!!

			// Click on the sort button
			updateContext.input.postEvent(MouseMoveEvent(sortRegion.minX, sortRegion.minY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(updateContext)

			val expectedInventory = Array<ItemStack?>(64) { null }
			expectedInventory[0] = ItemStack(longSword, 1)
			expectedInventory[1] = ItemStack(ironSword, 5)
			expectedInventory[2] = ItemStack(wingShield, 1)
			expectedInventory[3] = ItemStack(potion, 5)

			assertArrayEquals(expectedInventory, deuganState.inventory)

			val expectedMardekInventory = Array<ItemStack?>(64) { null }
			expectedMardekInventory[5] = ItemStack(wingShield, 1)
			assertArrayEquals(expectedMardekInventory, mardekState.inventory)

			// Clicking *again* shouldn't have any effect
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(updateContext)
			assertArrayEquals(expectedMardekInventory, mardekState.inventory)
			assertArrayEquals(expectedInventory, deuganState.inventory)
		}
	}
}
