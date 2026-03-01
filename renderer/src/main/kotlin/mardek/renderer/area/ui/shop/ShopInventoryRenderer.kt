package mardek.renderer.area.ui.shop

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.menu.inventory.InventoryRenderContext
import mardek.renderer.menu.inventory.renderItemStackAmount
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.actions.ShopInteractionState
import mardek.state.ingame.menu.inventory.ItemGridRenderInfo
import mardek.state.util.Rectangle

internal fun renderShopInventory(
	inventoryContext: InventoryRenderContext, scale: Int, region: Rectangle,
	interaction: ShopInteractionState,
) = inventoryContext.run {
	val baseSlotSize = 18
	val fullSlotSize = scale * baseSlotSize

	val numRows = region.height / fullSlotSize
	val numColumns = region.width / fullSlotSize

	val usedWidth = fullSlotSize * numColumns
	val offsetX = region.minX + (region.width - usedWidth) / 2

	val font = context.bundle.getFont(context.content.fonts.basic2.index)
	for (row in 0 until numRows) {
		for (column in 0 until numColumns) {
			val index = row * numColumns + column
			if (index >= interaction.shop.fixedItems.size) break

			val baseX = offsetX + column * fullSlotSize
			val baseY = region.minY + row * fullSlotSize

			var borderColor = rgb(208, 193, 142)
			var bottomColor = rgb(99, 78, 45)
			var topColor = rgb(69, 46, 29)
			if (interaction.hoveredShopInventoryIndex == index) {
				borderColor = rgb(165, 205, 254)
				bottomColor = rgb(25, 68, 118)
				topColor = rgb(64, 43, 36)
			}
			gradientWithBorder(
				colorBatch, baseX, baseY,
				baseX + fullSlotSize - 1, baseY + fullSlotSize - 1,
				1, 1,
				srgbToLinear(borderColor),
				srgbToLinear(bottomColor),
				srgbToLinear(bottomColor),
				srgbToLinear(topColor),
			)

			val itemX = baseX + scale
			val itemY = baseY + scale

			val fixedItem = interaction.shop.fixedItems[index]
			if (fixedItem != null) {
				val opacity = if (context.campaign.gold >= fixedItem.cost) 1f else 0.2f
				areaSpriteBatch!!.draw(fixedItem.sprite, itemX, itemY, scale, opacity = opacity)
			} else {
				val itemStack = context.campaign.shops.get(interaction.shop).inventory[index]
				if (itemStack != null) {
					areaSpriteBatch!!.draw(itemStack.item.sprite, itemX, itemY, scale)
					renderItemStackAmount(itemStack, itemX, itemY, scale, textBatch, font, skipOne = false)
				}
			}
		}
	}

	ItemGridRenderInfo(
		offsetX, region.minY, fullSlotSize,
		numRows, numColumns, interaction.shop.fixedItems.size - 1,
	)
}
