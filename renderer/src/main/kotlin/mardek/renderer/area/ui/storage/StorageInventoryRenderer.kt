package mardek.renderer.area.ui.storage

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.menu.inventory.InventoryRenderContext
import mardek.renderer.menu.inventory.renderItemStackAmount
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.actions.ItemStorageInteractionState
import mardek.state.ingame.actions.ItemStorageRenderInfo
import mardek.state.ingame.menu.inventory.ItemStorageSlotReference
import mardek.state.util.Rectangle

internal fun renderItemStorageInventory(
	inventoryContext: InventoryRenderContext, scale: Int, region: Rectangle,
	interaction: ItemStorageInteractionState,
) = inventoryContext.run {
	val baseSlotSize = 18
	val fullSlotSize = scale * baseSlotSize

	val numRows = region.height / fullSlotSize
	val numColumns = region.width / fullSlotSize
	val pageSize = numRows * numColumns

	val hoveredSlot = interaction.inventory.hoveredSlot
	val font = context.bundle.getFont(context.content.fonts.basic2.index)
	for (row in 0 until numRows) {
		for (column in 0 until numColumns) {
			val index = interaction.storagePage * pageSize + row * numColumns + column
			val baseX = region.minX + column * fullSlotSize
			val baseY = region.minY + row * fullSlotSize

			var borderColor = rgb(208, 193, 142)
			var bottomColor = rgb(99, 78, 45)
			var topColor = rgb(69, 46, 29)
			if (hoveredSlot is ItemStorageSlotReference && hoveredSlot.index == index) {
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

			if (index >= context.campaign.itemStorage.size) continue
			val itemStack = context.campaign.itemStorage[index] ?: continue

			val itemX = baseX + scale
			val itemY = baseY + scale
			spriteBatch.simple(itemX, itemY, scale, itemStack.item.sprite.index)
			renderItemStackAmount(itemStack, itemX, itemY, scale, textBatch, font)
		}
	}

	ItemStorageRenderInfo(
		interaction.storagePage * pageSize,
		region.minX, region.minY, fullSlotSize, numRows, numColumns,
	)
}
