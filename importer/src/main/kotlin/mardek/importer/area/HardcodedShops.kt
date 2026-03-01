package mardek.importer.area

import mardek.content.Content
import mardek.content.area.AreaShop
import mardek.content.inventory.Item
import java.util.UUID

internal fun hardcodeShops(content: Content) {
	content.areas.shops.add(goznorItemShop(content))
}

private fun item(content: Content, itemName: String) = content.items.items.find {
	it.displayName == itemName
} ?: throw RuntimeException("Can't find $itemName: options are ${content.items.items.map { it.displayName }}")

private fun goznorItemShop(content: Content): AreaShop {
	val fixedItems = Array<Item?>(60) { null }
	fixedItems[0] = item(content, "Potion")
	fixedItems[1] = item(content, "BetterPotion")
	fixedItems[2] = item(content, "Antidote")
	fixedItems[3] = item(content, "MotionPotion")
	fixedItems[4] = item(content, "HolyWater")
	fixedItems[5] = item(content, "LiquidSound")
	fixedItems[6] = item(content, "LiquidLight")
	fixedItems[7] = item(content, "BalloonJuice")
	fixedItems[8] = item(content, "Manaberry")
	fixedItems[9] = item(content, "PhoenixDown")
	fixedItems[10] = item(content, "CopperRing")

	return AreaShop(
		id = UUID.fromString("aa5b0942-2801-4e3a-8e79-440ae8207c2b"),
		name = "Goznor Item Shop",
		fixedItems = fixedItems,
		initialInventory = Array(60) { null },
	)
}
