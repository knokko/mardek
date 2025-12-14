package mardek.state.ingame.characters

import mardek.content.characters.CharacterState
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class TestCharacterState {

	@Test
	fun testGiveItem() {
		val item1 = Item()
		val item2 = Item()
		val state = CharacterState()

		for (index in state.inventory.indices) {
			state.inventory[index] = ItemStack(item1, 1)
		}

		assertTrue(state.giveItemStack(ItemStack(item1, 5)))
		val stack1 = state.inventory[0]!!
		assertSame(item1, stack1.item)
		assertEquals(6, stack1.amount)

		assertFalse(state.giveItemStack(ItemStack(item2, 1)))
		state.inventory[5] = null
		assertTrue(state.giveItemStack(ItemStack(item2, 2)))

		var stack5 = state.inventory[5]!!
		assertSame(item2, stack5.item)
		assertEquals(2, stack5.amount)

		state.inventory[2] = null
		assertTrue(state.giveItemStack(ItemStack(item2, 8)))
		assertNull(state.inventory[2])
		stack5 = state.inventory[5]!!
		assertSame(item2, stack5.item)
		assertEquals(10, stack5.amount)
	}

	@Test
	fun testRemoveItem() {
		val item1 = Item()
		val item2 = Item()

		val state = CharacterState()
		state.inventory[2] = ItemStack(item1, 2)
		assertFalse(state.removeItem(item2))
		assertTrue(state.removeItem(item1))

		val stack1 = state.inventory[2]!!
		assertSame(item1, stack1.item)
		assertEquals(1, stack1.amount)

		assertTrue(state.removeItem(item1))
		assertNull(state.inventory[2])

		assertFalse(state.removeItem(item1))
	}
}
