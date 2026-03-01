package mardek.state.ingame.area

import com.github.knokko.bitser.Bitser
import mardek.content.area.AreaContent
import mardek.content.area.AreaShop
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.inventory.ItemsContent
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class TestShopsStates {

	@Test
	fun testSaveMapping() {
		val item1 = Item()
		val item2 = Item()

		val unusedShop = AreaShop(
			UUID.randomUUID(), "unused",
			Array(60) { null }, Array(60) { null }
		)
		val readOnlyShop = AreaShop(
			UUID.randomUUID(), "read-only",
			arrayOf(item1, null) + Array(58) { null },
			arrayOf(null, ItemStack(item2, 2)) + Array(58) { null },
		)
		val modifiedShop = AreaShop(
			UUID.randomUUID(), "modified",
			arrayOf(null, item2) + Array(58) { null },
			Array(60) { null }
		)
		val resetShop = AreaShop(
			UUID.randomUUID(), "reset",
			Array(60) { null },
			arrayOf<ItemStack?>(ItemStack(item1, 1)) + Array(59) { null },
		)

		val areaContent = AreaContent()
		areaContent.shops.addAll(listOf(unusedShop, readOnlyShop, modifiedShop, resetShop))

		val itemsContent = ItemsContent()
		itemsContent.items.addAll(listOf(item1, item2))

		val original = ShopsStates()
		original.get(readOnlyShop)
		original.get(modifiedShop).inventory[0] = ItemStack(item1, 1)
		original.get(resetShop).inventory[0] = null
		original.get(resetShop).inventory[0] = ItemStack(item1, 1)

		val bitser = Bitser()
		val bytes = bitser.toBytes(original, areaContent, itemsContent)

		// Do a simple copy, without changing the shop definitions
		val simpleCopy = bitser.fromBytes(
			ShopsStates::class.java, bytes, areaContent, itemsContent
		)
		assertArrayEquals(
			Array(60) { null },
			simpleCopy.get(unusedShop).inventory,
		)
		assertArrayEquals(
			arrayOf(null, ItemStack(item2, 2)) + Array(58) { null },
			simpleCopy.get(readOnlyShop).inventory,
		)
		assertArrayEquals(
			arrayOf(ItemStack(item1, 1), null) + Array(58) { null },
			simpleCopy.get(modifiedShop).inventory,
		)
		assertArrayEquals(
			arrayOf<ItemStack?>(ItemStack(item1, 1)) + Array(59) { null },
			simpleCopy.get(resetShop).inventory,
		)

		// Change the shop definitions
		unusedShop.initialInventory[0] = ItemStack(item2, 5)
		readOnlyShop.initialInventory[1] = ItemStack(item2, 6)
		modifiedShop.initialInventory[1] = ItemStack(item2, 7)
		resetShop.initialInventory[0] = ItemStack(item2, 8)

		// Deserialize again
		val complexCopy = bitser.fromBytes(
			ShopsStates::class.java, bytes, areaContent, itemsContent
		)

		// Since only the inventory of modifiedShop has actual changes, that's the only shop inventory that should
		// have been saved: the other 3 shops should get their new inventory
		assertArrayEquals(
			arrayOf<ItemStack?>(ItemStack(item2, 5)) + Array(59) { null },
			complexCopy.get(unusedShop).inventory,
		)
		assertArrayEquals(
			arrayOf(null, ItemStack(item2, 6)) + Array(58) { null },
			complexCopy.get(readOnlyShop).inventory,
		)
		assertArrayEquals(
			arrayOf(ItemStack(item1, 1), null) + Array(58) { null },
			complexCopy.get(modifiedShop).inventory,
		)
		assertArrayEquals(
			arrayOf<ItemStack?>(ItemStack(item2, 8)) + Array(59) { null },
			complexCopy.get(resetShop).inventory,
		)
	}
}
