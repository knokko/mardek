package mardek.game.ui

import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.ItemStack
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
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.inventory.InventoryTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestInventory {

	fun testMoveEquipment(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState.loadChapter(content, 1), "")
			assertSame(
				content.items.items.find { it.displayName == "Hero's Armour" }!!,
				state.campaign.characterStates[heroMardek]!!.equipment[heroMardek.characterClass.equipmentSlots[3]]
			)
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds)

			context.input.postEvent(pressKeyEvent(InputKey.Interact)) // Skip chapter number
			context.input.postEvent(repeatKeyEvent(InputKey.Interact)) // Skip intro cutscene
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(context)

			// Skip intro dialogue
			context.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(context)
			}
			context.input.postEvent(releaseKeyEvent(InputKey.Cancel))

			// Open inventory
			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(context)
			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(context)

			assertSame(content.audio.fixedEffects.ui.openMenu, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.scroll1, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.scroll1, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			val baseColors = arrayOf(
				Color(177, 171, 171, 255), // Hero Mardek sprite armor
				Color(91, 141, 46), // Hero Deugan sprite robe
				Color(51, 51, 204), // Crystal pointer
				Color(233, 233, 233), // Hero Armour
				Color(255, 204, 102), // Balmung
				Color(183, 142, 0), // Elixir
				Color(179, 162, 117), // Item grid border color
				Color(141, 103, 49), // Clock color
			)
			val forbiddenSlotColor = arrayOf(
				Color(255, 162, 162),
			)

			// Create render info
			testRendering(
				state, 900, 700, "inventory-drag1",
				baseColors, forbiddenSlotColor,
			)
			val tab = state.menu.currentTab as InventoryTab

			val mardekRenderInfo = tab.equipmentRenderInfo.toList()[0]
			val deuganRenderInfo = tab.equipmentRenderInfo.toList()[1]
			val gridRenderInfo = tab.gridRenderInfo!!

			// Pick up the Dragon Amulet of Mardek
			context.input.postEvent(MouseMoveEvent(
				mardekRenderInfo.startX + 4 * mardekRenderInfo.slotSpacing, mardekRenderInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Swap the Dragon Amulet with the Elixirs of Deugan
			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			context.input.postEvent(MouseMoveEvent(gridRenderInfo.startX, gridRenderInfo.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.scroll2, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Put the Elixirs of Deugan in the next slot
			context.input.postEvent(MouseMoveEvent(
				gridRenderInfo.startX + gridRenderInfo.slotSize, gridRenderInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickCancel, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Put the Dragon Amulet (of Mardek) in the second accessory slot of Deugan
			context.input.postEvent(MouseMoveEvent(gridRenderInfo.startX, gridRenderInfo.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(
				deuganRenderInfo.startX + 5 * deuganRenderInfo.slotSpacing, deuganRenderInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickCancel, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Attempt to grab the sword of Mardek, which is forbidden
			context.input.postEvent(MouseMoveEvent(mardekRenderInfo.startX, mardekRenderInfo.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Grab the shield of Mardek, and try to give it to Deugan
			context.input.postEvent(MouseMoveEvent(
				mardekRenderInfo.startX + mardekRenderInfo.slotSpacing, mardekRenderInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(
				deuganRenderInfo.startX + deuganRenderInfo.slotSpacing, deuganRenderInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Should render the slot red
			testRendering(
				state, 900, 700, "inventory-drag2",
				baseColors + forbiddenSlotColor, emptyArray(),
			)

			fun assertEquipment(owner: PlayableCharacter, slot: Int, itemName: String?) {
				val ownerState = state.campaign.characterStates[owner]!!
				val actualItem = ownerState.equipment[owner.characterClass.equipmentSlots[slot]]
				if (itemName == null) assertNull(actualItem)
				else assertSame(content.items.items.find { it.displayName == itemName }!!, actualItem)
			}
			assertEquipment(heroMardek, 0, "M Blade")
			assertEquipment(heroMardek, 1, null)
			assertEquipment(heroMardek, 4, null)
			assertEquipment(heroDeugan, 0, "Balmung")
			assertEquipment(heroDeugan, 1, null)
			assertEquipment(heroDeugan, 4, "Dragon Amulet")
			assertEquipment(heroDeugan, 5, "Dragon Amulet")
			assertEquals(
				ItemStack(content.items.items.find { it.displayName == "Hero's Shield" }!!, 1),
				state.campaign.cursorItemStack,
			)
			assertEquals(ItemStack(elixir, 9), state.campaign.characterStates[heroMardek]!!.inventory[0])
			assertNull(state.campaign.characterStates[heroMardek]!!.inventory[1])
			assertEquals(ItemStack(elixir, 9), state.campaign.characterStates[heroDeugan]!!.inventory[1])
			assertNull(state.campaign.characterStates[heroDeugan]!!.inventory[0])
		}
	}

	fun testEquipmentStatsHints(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 1.milliseconds)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)

			repeat(2) {
				context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(context)
			}

			val baseColors = arrayOf(
				Color(238, 203, 127), // Unselected stats text color (for Deugan)
				Color(164, 204, 253), // Selected base stats text color (for Mardek)
			)
			val equalColor = arrayOf(Color(0, 220, 255))
			val increaseColor = arrayOf(Color(152, 255, 0))
			val decreaseColor = arrayOf(Color(255, 85, 85))
			testRendering(
				state, 1200, 900, "equipment-stats0",
				baseColors, equalColor + increaseColor + decreaseColor,
			)

			val inventory = state.campaign.characterStates[heroMardek]!!.inventory
			inventory[5] = ItemStack(content.items.items.find { it.displayName == "Blood Sword" }!!, 1)
			inventory[6] = ItemStack(content.items.items.find { it.displayName == "Tunic" }!!, 1)
			inventory[7] = ItemStack(content.items.items.find { it.displayName == "Hero's Armour" }!!, 1)

			// Hover over the Blood Sword, which is much weaker than the M Blade,
			// so the attack stat should be colored red
			val tab = state.menu.currentTab as InventoryTab
			val grid = tab.gridRenderInfo!!
			context.input.postEvent(MouseMoveEvent(grid.startX + 5 * grid.slotSize, grid.startY))
			state.update(context)
			testRendering(
				state, 1200, 900, "equipment-stats1",
				baseColors + decreaseColor, equalColor + increaseColor,
			)

			// Hover over the Tunic, which shouldn't do anything, since Hero Mardek & Hero Deugan cannot equip it
			context.input.postEvent(MouseMoveEvent(grid.startX + 6 * grid.slotSize, grid.startY))
			state.update(context)
			testRendering(
				state, 1200, 900, "equipment-stats2",
				baseColors, equalColor + increaseColor + decreaseColor,
			)

			// Hover over Hero's Armour, which should color the DEF of Mardek green
			context.input.postEvent(MouseMoveEvent(grid.startX + 7 * grid.slotSize, grid.startY))
			state.update(context)
			testRendering(
				state, 1200, 900, "equipment-stats3",
				baseColors + increaseColor, equalColor + decreaseColor,
			)

			// Hover over the M Blade, which should color the ATK of Mardek blue
			val equipmentInfo = tab.equipmentRenderInfo.iterator().next()
			context.input.postEvent(MouseMoveEvent(equipmentInfo.startX, equipmentInfo.startY))
			state.update(context)
			testRendering(
				state, 1200, 900, "equipment-stats4",
				baseColors + equalColor, increaseColor + decreaseColor,
			)
		}
	}

	fun testDiscardItem(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 1.milliseconds)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)

			repeat(2) {
				context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(context)
			}

			state.campaign.characterStates[heroMardek]!!.inventory[1] = ItemStack(elixir, 5)

			val baseColors = arrayOf(
				Color(153, 153, 153), // Light thrash color
				Color(50, 50, 50), // Dark thrash color
				Color(179, 162, 117), // Item grid border color
				Color(255, 204, 102), // Balmung handle color
			)
			val elixirColors = arrayOf(
				Color(247, 236, 0),
				Color(183, 142, 0),
			)
			testRendering(
				state, 500, 300, "discard-item0",
				baseColors + elixirColors, emptyArray(),
			)

			val tab = state.menu.currentTab as InventoryTab
			context.input.postEvent(MouseMoveEvent(
				tab.gridRenderInfo!!.startX + tab.gridRenderInfo!!.slotSize, tab.gridRenderInfo!!.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(releaseKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(
				tab.thrashRegion!!.minX, tab.thrashRegion!!.minY
			))
			state.update(context)
			testRendering(
				state, 500, 300, "discard-item1",
				baseColors + elixirColors, emptyArray(),
			)

			// Empty sound queue
			while (context.soundQueue.take() != null) {
				context.soundQueue.take()
			}

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			testRendering(
				state, 500, 300, "discard-item2",
				baseColors, elixirColors,
			)
			assertNull(state.campaign.characterStates[heroMardek]!!.inventory[1])
			assertNull(state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickCancel, context.soundQueue.take())
			assertNull(context.soundQueue.take())
		}
	}

	fun testSplitItemStack(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 1.milliseconds)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)

			repeat(2) {
				context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(context)
			}

			val inventory = state.campaign.characterStates[heroMardek]!!.inventory
			inventory[1] = ItemStack(elixir, 3)
			testRendering(
				state, 400, 300, "split-stack",
				emptyArray(), emptyArray(),
			)
			while (context.soundQueue.take() != null) {
				context.soundQueue.take()
			}

			val tab = state.menu.currentTab as InventoryTab
			val grid = tab.gridRenderInfo!!

			// Split the elixir stack 3 times, which should cause the entire stack to be taken (in 3 steps)
			context.input.postEvent(MouseMoveEvent(grid.startX + grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertEquals(ItemStack(elixir, 2), inventory[1])
			assertEquals(ItemStack(elixir, 1), state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertEquals(ItemStack(elixir, 1), inventory[1])
			assertEquals(ItemStack(elixir, 2), state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertNull(inventory[0])
			assertEquals(ItemStack(elixir, 3), state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Split-clicking a now-empty slot should not have any effect
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertNull(inventory[0])
			assertEquals(ItemStack(elixir, 3), state.campaign.cursorItemStack)
			assertNull(context.soundQueue.take())

			// Put the elixir stack back into the inventory
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(ItemStack(elixir, 3), inventory[1])
			assertNull(state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickCancel, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Try to split-take the weapon from Mardek, which should fail
			val equipmentInfo = tab.equipmentRenderInfo.iterator().next()
			val equipment = state.campaign.characterStates[heroMardek]!!.equipment
			val slots = heroMardek.characterClass.equipmentSlots
			val shield = content.items.items.find { it.displayName == "Hero's Shield" }!!
			equipment[slots[1]] = shield
			equipment[slots[3]] = content.items.items.find { it.displayName == "Hero's Armour" }!!
			context.input.postEvent(MouseMoveEvent(equipmentInfo.startX, equipmentInfo.startY))
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertNull(state.campaign.cursorItemStack)
			assertSame(content.items.items.find { it.displayName == "M Blade" }!!, equipment[slots[0]])
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Split-take the shield from Mardek
			context.input.postEvent(MouseMoveEvent(
				equipmentInfo.startX + equipmentInfo.slotSpacing, equipmentInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertNull(equipment[slots[1]])
			assertEquals(ItemStack(shield, 1), state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			// Try to split-take the empty helmet slot, which should not do anything
			context.input.postEvent(MouseMoveEvent(
				equipmentInfo.startX + 2 * equipmentInfo.slotSpacing, equipmentInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertNull(equipment[slots[2]])
			assertEquals(ItemStack(shield, 1), state.campaign.cursorItemStack)
			assertNull(context.soundQueue.take())

			// Try to split-take the chestplate from Mardek, which is not allowed
			context.input.postEvent(MouseMoveEvent(
				equipmentInfo.startX + 3 * equipmentInfo.slotSpacing, equipmentInfo.startY
			))
			context.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			state.update(context)
			assertSame(content.items.items.find { it.displayName == "Hero's Armour" }!!, equipment[slots[3]])
			assertEquals(ItemStack(shield, 1), state.campaign.cursorItemStack)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
		}
	}

	fun testConsumeItems(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 1.seconds)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)

			repeat(2) {
				context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(context)
			}

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50
			val maxHealth = mardekState.determineMaxHealth(heroMardek.baseStats, emptySet())
			val maxMana = mardekState.determineMaxMana(heroMardek.baseStats, emptySet())
			mardekState.currentHealth = maxHealth
			mardekState.currentMana = maxMana
			val potion = content.items.items.find { it.displayName == "Potion" }!!
			val ether = content.items.items.find { it.displayName == "Ether" }!!
			val oxyale = content.items.items.find { it.displayName == "Oxyale" }!!
			val antidote = content.items.items.find { it.displayName == "Antidote" }!!
			val remedy = content.items.items.find { it.displayName == "Remedy" }!!
			val bomb = content.items.items.find { it.displayName == "Noxious Bomb" }!!
			mardekState.inventory[0] = ItemStack(elixir, 2)
			mardekState.inventory[1] = ItemStack(potion, 5)
			mardekState.inventory[2] = ItemStack(ether, 5)
			mardekState.inventory[3] = ItemStack(oxyale, 2)
			mardekState.inventory[4] = ItemStack(antidote, 2)
			mardekState.inventory[5] = ItemStack(remedy, 2)
			mardekState.inventory[6] = ItemStack(bomb, 1)
			testRendering(
				state, 400, 300, "consume-inventory-items",
				emptyArray(), emptyArray(),
			)
			while (context.soundQueue.take() != null) {
				context.soundQueue.take()
			}

			val tab = state.menu.currentTab as InventoryTab
			val consumeRegion = tab.equipmentRenderInfo.first().consumableRegion
			val grid = tab.gridRenderInfo!!
			val cureSound = content.audio.effects.find { it.flashName == "Cure" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val aquaLung = content.stats.statusEffects.find { it.flashName == "UWB" }!!

			// Try to feed Elixir to full-health Mardek, which should fail
			context.input.postEvent(MouseMoveEvent(grid.startX, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(elixir, 2), state.campaign.cursorItemStack)

			// Feed elixir to Mardek with missing health
			mardekState.currentHealth -= 123
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxHealth, mardekState.currentHealth)
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(elixir, 1), state.campaign.cursorItemStack)

			// Feed elixir to Mardek with missing mana
			mardekState.currentMana = 12
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxMana, mardekState.currentMana)
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertNull(state.campaign.cursorItemStack)

			// Feed an empty item stack to Mardek, which shouldn't do anything
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxMana, mardekState.currentMana)
			assertNull(context.soundQueue.take())

			// Feed potions to Mardek until he is full health
			mardekState.currentHealth -= 250
			context.input.postEvent(MouseMoveEvent(grid.startX + grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(potion, 4), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(potion, 3), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxHealth, mardekState.currentHealth)
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(potion, 2), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxHealth, mardekState.currentHealth)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(potion, 2), state.campaign.cursorItemStack)

			// Feed ethers to Mardek
			mardekState.currentMana -= 90
			context.input.postEvent(MouseMoveEvent(grid.startX + 2 * grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxMana, mardekState.currentMana)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(ether, 4), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(maxMana, mardekState.currentMana)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(ether, 4), state.campaign.cursorItemStack)

			// Feed oxyale to Mardek
			context.input.postEvent(MouseMoveEvent(grid.startX + 3 * grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			assertEquals(0, mardekState.activeStatusEffects.size)
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(oxyale, 1), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(oxyale, 1), state.campaign.cursorItemStack)

			// Feed Antidote to Mardek
			mardekState.activeStatusEffects.add(poison)
			context.input.postEvent(MouseMoveEvent(grid.startX + 4 * grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			assertEquals(setOf(aquaLung, poison), mardekState.activeStatusEffects)
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(antidote, 1), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(antidote, 1), state.campaign.cursorItemStack)

			// Feed remedies to Mardek
			mardekState.activeStatusEffects.add(poison)
			context.input.postEvent(MouseMoveEvent(grid.startX + 5 * grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			assertEquals(setOf(aquaLung, poison), mardekState.activeStatusEffects)
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(cureSound, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(remedy, 1), state.campaign.cursorItemStack)

			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(remedy, 1), state.campaign.cursorItemStack)

			// Attempt to give Mardek a noxious bomb
			context.input.postEvent(MouseMoveEvent(grid.startX + 6 * grid.slotSize, grid.startY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			context.input.postEvent(MouseMoveEvent(consumeRegion.minX, consumeRegion.minY))
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(context)
			assertEquals(setOf(aquaLung), mardekState.activeStatusEffects)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())
			assertNull(context.soundQueue.take())
			assertEquals(ItemStack(bomb, 1), state.campaign.cursorItemStack)
		}
	}
}
