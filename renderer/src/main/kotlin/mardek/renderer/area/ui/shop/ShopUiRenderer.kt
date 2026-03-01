package mardek.renderer.area.ui.shop

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.menu.inventory.InventoryRenderContext
import mardek.renderer.menu.inventory.SIMPLE_SLOT_SIZE
import mardek.renderer.menu.inventory.renderCharacterBars
import mardek.renderer.menu.inventory.renderHoverItemProperties
import mardek.renderer.menu.inventory.renderInventoryOverlay
import mardek.renderer.menu.inventory.renderItemGrid
import mardek.state.ingame.actions.ShopInteractionState
import mardek.state.util.Rectangle
import kotlin.math.min

internal fun renderShopUi(
	context: RenderContext, state: ShopInteractionState, region: Rectangle
) = state.run {
	val colorBatch = context.addColorBatch(2000) // There are quite some slots to render...
	val spriteBatch = context.addAreaSpriteBatch(500, region) // Character slots, item slots, equipment slots...
	val imageBatch = context.addImageBatch(50) // Only a couple of icons
	val lateColorBatch = context.addColorBatch(2) // Only for equipment slot name tooltips
	val textBatch = context.addFancyTextBatch(2000) // Item descriptions can be long

	val splitX = region.minX + 7 * region.width / 10
	val barHeight = region.height / 12
	val barY = region.minY + barHeight

	val barColor = srgbToLinear(rgb(24, 14, 10))
	colorBatch.fill(region.minX, region.minY, splitX - 1, barY, barColor)
	colorBatch.fill(
		region.minX, barY - region.height / 500, splitX - 1, barY,
		srgbToLinear(rgb(68, 51, 34))
	)
	colorBatch.gradient(
		region.minX, barY + 1, region.maxX, region.maxY,
		srgbToLinear(rgb(47, 33, 20)),
		srgbToLinear(rgb(88, 58, 36)),
		srgbToLinear(rgb(45, 32, 19)),
	)
	colorBatch.gradient(
		splitX + 1, region.minY, region.maxX, region.maxY,
		srgbToLinear(rgb(129, 83, 53)),
		srgbToLinear(rgb(129, 83, 53)),
		srgbToLinear(rgb(35, 21, 15)),
	)
	colorBatch.fill(
		splitX, region.minY, splitX + region.width / 500, region.maxY,
		srgbToLinear(rgb(152, 105, 61)),
	)

	val baseSlotSize = 18
	val itemScale = min((splitX - region.minX) / baseSlotSize / 14, region.height / baseSlotSize / 13)

	val inventoryContext = InventoryRenderContext(
		context, colorBatch, null, spriteBatch, imageBatch, lateColorBatch, textBatch
	)

	if (itemScale > 0) {
		renderedCharacterBars = renderCharacterBars(
			inventoryContext, inventory, context.campaign.usedPartyMembers(),
			region.minX + 5 * itemScale, barY + 3 * itemScale, splitX - 1, itemScale,
		).toTypedArray()

		val shopRegion = Rectangle(
			splitX, region.minY + region.height / 10,
			region.boundX - splitX, region.boundY - region.minY - region.height / 6,
		)
		renderedShopInventory = renderShopInventory(inventoryContext, itemScale, shopRegion, state)
		updateHoveredShopSlot()

		val gridOffset = 2 + 3 * itemScale + 8 * itemScale * SIMPLE_SLOT_SIZE
		val gridStartX = splitX - 1 - gridOffset
		val inventoryY = region.boundY - gridOffset
		val selectedCharacter = context.campaign.allPartyMembers()[inventory.partyIndex]
		if (selectedCharacter != null) {
			val infoMaxX = min(region.minX + 200 * itemScale, gridStartX - 2 * itemScale)
			val hoveredShopItem = if (hoveredShopInventoryIndex != -1) {
				val shopState = context.campaign.shops.get(shop)
				shop.fixedItems[hoveredShopInventoryIndex] ?: shopState.inventory[hoveredShopInventoryIndex]?.item
			} else null
			renderHoverItemProperties(
				inventoryContext, inventory, selectedCharacter.second,
				region.minX, inventoryY, infoMaxX, region.maxY, itemScale,
				defaultHoverItem = hoveredShopItem,
			)
			renderedCharacterInventory = renderItemGrid(
				inventoryContext, selectedCharacter.second.inventory, inventory,
				gridStartX, region.boundY - gridOffset, itemScale,
			)
		}
	}

	val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
	textBatch.drawString(
		"Inventory", region.minX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, upperFont, srgbToLinear(rgb(131, 81, 37)),
	)
	textBatch.drawString(
		"Shop", splitX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, upperFont, srgbToLinear(rgb(238, 203, 127)),
	)

	val simpleFont = context.bundle.getFont(context.content.fonts.basic2.index)
	textBatch.drawString(
		"Value:", splitX + region.height * 0.12f, region.boundY - region.height * 0.03f,
		region.height * 0.025f, simpleFont,
		srgbToLinear(rgb(207, 192, 141)),
		srgbToLinear(rgb(53, 34, 22)),
		region.height * 0.001f, TextAlignment.RIGHT,
	)

	val hoveredItemValue = run {
		var result = 0
		val inventoryItem = inventory.hoveredSlot?.get()?.item
		if (inventoryItem != null) result = inventoryItem.cost
		if (hoveredShopInventoryIndex != -1) {
			val shopItem = shop.fixedItems[hoveredShopInventoryIndex]
			if (shopItem != null) {
				result = shopItem.cost
			} else {
				val shopStack = context.campaign.shops.get(shop).inventory[hoveredShopInventoryIndex]
				if (shopStack != null) result = shopStack.item.cost
			}
		}
		result
	}

	val valueFont = context.bundle.getFont(context.content.fonts.large1.index)
	textBatch.drawShadowedString(
		hoveredItemValue.toString(), splitX + region.height * 0.13f,
		region.boundY - region.height * 0.025f, region.height * 0.03f, valueFont,
		srgbToLinear(rgb(255, 225, 124)), 0, 0f,
		srgbToLinear(rgb(60, 40, 25)),
		0.003f * region.height, 0.003f * region.height, TextAlignment.LEFT,
	)

	val overlayRegion = Rectangle(region.minX, barY, splitX, region.maxY)
	thrashRegion = renderInventoryOverlay(
		inventoryContext, overlayRegion, itemScale, context.campaign, inventory,
		renderedCharacterBars.toList(), thrashRegion,
	)

	renderShopTradeOverlay(context, this, region)

	Pair(colorBatch, textBatch)
}
