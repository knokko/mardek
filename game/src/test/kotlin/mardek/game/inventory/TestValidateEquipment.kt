package mardek.game.inventory

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull

object TestValidateEquipment {

	fun testInvalidEquipmentIsErased(instance: TestingInstance) {
		instance.apply {
			val state = CampaignState.loadChapter(content, 1)
			val mardekState = state.characterStates[heroMardek]!!
			val slot = heroMardek.characterClass.equipmentSlots[3]
			assertEquals("Hero's Armour", mardekState.equipment[slot]!!.displayName)

			val tunic = content.items.items.find { it.displayName == "Tunic" }!!
			mardekState.equipment[slot] = tunic // Hero Mardek is not allowed to equip the Tunic
			val itemStorage = ArrayList<ItemStack?>()
			mardekState.initialize(heroMardek, itemStorage)
			assertNull(mardekState.equipment[slot])
			assertEquals(arrayListOf(ItemStack(tunic, 1)), itemStorage)
		}
	}

	fun testEquipmentInSlotsOfSomeoneElse(instance: TestingInstance) {
		instance.apply {
			val state = CampaignState.loadChapter(content, 1)
			val mardekState = state.characterStates[heroMardek]!!
			val deuganSlot = heroDeugan.characterClass.equipmentSlots[3]
			assertNull(mardekState.equipment[deuganSlot])

			val item = content.items.items.find { deuganSlot.itemTypes.contains(it.type) }!!
			mardekState.equipment[deuganSlot] = item
			val itemStorage = ArrayList<ItemStack?>()
			mardekState.initialize(heroMardek, itemStorage)
			assertNull(mardekState.equipment[deuganSlot])
			assertEquals(arrayListOf(ItemStack(item, 1)), itemStorage)
		}
	}

	fun testEquipmentSlotDefinitions(instance: TestingInstance) {
		instance.apply {
			val slots = heroMardek.characterClass.equipmentSlots
			assertEquals("WEAPON: SWORD", slots[0].itemTypes.joinToString { it.displayName })
			assertFalse(slots[0].canBeEmpty)
			assertEquals("SHIELD", slots[1].itemTypes.joinToString { it.displayName })
			for (index in 1 until slots.size) assertTrue(slots[index].canBeEmpty)
		}
	}
}
