package mardek.state.ingame.actions

import mardek.content.area.AreaShop
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.inventory.ItemType
import mardek.content.sprite.KimSprite
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.ShopState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.UUID

class TestShopInteractionState {

	private fun dummyItem(cost: Int) = Item(
		UUID.randomUUID(), "item$cost", KimSprite(), "",
		ItemType(), null, cost, null, null,
	)

	@Test
	fun testInitiate() {
		val shop = AreaShop(
			UUID.randomUUID(), "test",
			Array(60) { null },
			Array(60) { null },
		)
		val shopState = ShopState(shop)

		val item1 = dummyItem(100)
		val item2 = dummyItem(200)

		// No trade is possible without any items
		assertNull(PendingTrade.initiate(0, null, 0, shop, shopState))
		assertNull(PendingTrade.initiate(12345, null, 0, shop, shopState))

		// If we add an item to the shop inventory, trading is possible, if the player has enough money
		shopState.inventory[0] = ItemStack(item1, 5)
		assertNull(PendingTrade.initiate(0, null, 0, shop, shopState))
		assertNull(PendingTrade.initiate(99, null, 0, shop, shopState))
		assertEquals(
			PendingBuyStack(0),
			PendingTrade.initiate(100, null, 0, shop, shopState)
		)
		assertNull(PendingTrade.initiate(12345, null, 1, shop, shopState))

		// If we add an endless-supply item to the shop, it takes precedence over the shop inventory
		shop.fixedItems[0] = item2
		assertNull(PendingTrade.initiate(199, null, 0, shop, shopState))
		assertEquals(
			PendingBuyItem(item2),
			PendingTrade.initiate(200, null, 0, shop, shopState)
		)
		assertNull(PendingTrade.initiate(12345, null, 1, shop, shopState))

		// If we put another item on the cursor, no trade is possible
		assertNull(PendingTrade.initiate(
			12345, ItemStack(item1, 1), 0, shop, shopState
		))

		// But, if we put the same item on the cursor, we can still buy
		assertEquals(PendingBuyItem(item2), PendingTrade.initiate(
			200, ItemStack(item2, 1), 0, shop, shopState
		))

		// Finally, if the shop slot is empty, we can sell the cursor stack
		assertEquals(PendingSell(1), PendingTrade.initiate(
			0, ItemStack(item2, 1), 1, shop, shopState
		))
	}

	@Test
	fun testBuyItem() {
		val item = dummyItem(100)
		val shop = AreaShop()
		val shopState = ShopState(shop)

		val buyItem = PendingBuyItem(item)
		assertEquals(1, buyItem.getMaxAmount(199, null, shopState))
		assertEquals(1, buyItem.getMaxAmount(
			199, ItemStack(item, 1), shopState
		))
		assertEquals(2, buyItem.getMaxAmount(200, null, shopState))

		assertSame(item, buyItem.item(null, shopState))
		assertSame(item, buyItem.item(ItemStack(item, 1), shopState))

		val campaign = CampaignState()
		campaign.gold = 1000
		buyItem.amount = 5
		buyItem.execute(campaign, shop)

		assertEquals(ItemStack(item, 5), campaign.cursorItemStack)
		assertEquals(500, campaign.gold)

		buyItem.execute(campaign, shop)
		assertEquals(ItemStack(item, 10), campaign.cursorItemStack)
		assertEquals(0, campaign.gold)
	}

	@Test
	fun testBuyStack() {
		val item = dummyItem(100)
		val otherItem = dummyItem(12345)
		val shop = AreaShop(
			UUID.randomUUID(), "test buy stack", arrayOf(otherItem, null, null),
			arrayOf(null, ItemStack(otherItem, 1), ItemStack(item, 5))
		)
		val campaign = CampaignState()
		val shopState = campaign.shops.get(shop)

		val buyStack = PendingBuyStack(2)

		assertEquals(1, buyStack.getMaxAmount(199, null, shopState))
		assertEquals(2, buyStack.getMaxAmount(200, null, shopState))

		// Only 5 are in stock
		assertEquals(5, buyStack.getMaxAmount(1000, null, shopState))
		assertEquals(5, buyStack.getMaxAmount(
			1000, ItemStack(item, 1), shopState
		))

		assertSame(item, buyStack.item(null, shopState))
		assertSame(item, buyStack.item(ItemStack(item, 1), shopState))

		campaign.gold = 1000
		buyStack.amount = 2
		buyStack.execute(campaign, shop)

		assertEquals(ItemStack(item, 2), campaign.cursorItemStack)
		assertEquals(800, campaign.gold)
		assertEquals(ItemStack(item, 3), shopState.inventory[2])

		buyStack.amount = 3
		buyStack.execute(campaign, shop)
		assertEquals(ItemStack(item, 5), campaign.cursorItemStack)
		assertEquals(500, campaign.gold)
		assertNull(shopState.inventory[2])
	}

	@Test
	fun testSell() {
		val item = dummyItem(100)
		val otherItem = dummyItem(500)
		val shop = AreaShop(
			UUID.randomUUID(), "test buy stack", arrayOf(otherItem, null, null),
			arrayOf(null, null, ItemStack(item, 5))
		)
		val campaign = CampaignState()
		val shopState = campaign.shops.get(shop)

		val sellToEmptySlot = PendingSell(1)
		val sellToFilledSlot = PendingSell(2)
		assertEquals(1, sellToEmptySlot.getMaxAmount(
			12, ItemStack(item, 1), shopState
		))
		assertEquals(1, sellToFilledSlot.getMaxAmount(
			12, ItemStack(item, 1), shopState
		))
		assertEquals(123, sellToEmptySlot.getMaxAmount(
			12, ItemStack(item, 123), shopState
		))
		assertEquals(123, sellToFilledSlot.getMaxAmount(
			12, ItemStack(item, 123), shopState
		))

		assertSame(item, sellToEmptySlot.item(ItemStack(item, 1), shopState))
		assertSame(otherItem, sellToEmptySlot.item(
			ItemStack(otherItem, 1), shopState
		))
		assertSame(item, sellToFilledSlot.item(ItemStack(item, 1), shopState))

		campaign.cursorItemStack = ItemStack(item, 5)
		sellToEmptySlot.amount = 2
		sellToEmptySlot.execute(campaign, shop)
		assertEquals(ItemStack(item, 3), campaign.cursorItemStack)
		assertEquals(100, campaign.gold)
		assertEquals(ItemStack(item, 2), shopState.inventory[1])

		sellToFilledSlot.amount = 3
		sellToFilledSlot.execute(campaign, shop)
		assertNull(campaign.cursorItemStack)
		assertEquals(250, campaign.gold)
		assertEquals(ItemStack(item, 8), shopState.inventory[2])
	}
}
