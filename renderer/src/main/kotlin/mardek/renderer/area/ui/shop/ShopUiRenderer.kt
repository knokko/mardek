package mardek.renderer.area.ui.shop

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
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
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun renderShopUi(
	context: RenderContext, state: ShopInteractionState, rawRegion: Rectangle
) = state.run {
	val colorBatch = context.addColorBatch(2000) // There are quite some slots to render...
	val spriteBatch = context.addAreaSpriteBatch(500, rawRegion) // Character slots, item slots, equipment slots...
	val imageBatch = context.addImageBatch(50) // Only a couple of icons
	val lateColorBatch = context.addColorBatch(2) // Only for equipment slot name tooltips
	val simpleTextBatch = context.addTextBatch(2000) // Item descriptions can be long...
	val fancyTextBatch = context.addFancyTextBatch(200) // Only for rendering "MASTERED'

	val splitX = rawRegion.minX + 7 * rawRegion.width / 10
	val barHeight = rawRegion.height / 12
	val barY = rawRegion.minY + barHeight

	var transitionProgress = context.timing.interpolate(
		startedFadeInAt, 0f,
		ShopInteractionState.FADE_DURATION, 1f, true
	)
	if (startedFadeOutAt != null) {
		transitionProgress *= context.timing.interpolate(
			startedFadeOutAt!!, 1f,
			ShopInteractionState.FADE_DURATION, 0f, true
		)
	}
	val fadeStrength = 1f - transitionProgress

	val transitionAlpha = (255f * transitionProgress).roundToInt()

	val barColor = srgbToLinear(rgba(24, 14, 10, transitionAlpha))
	colorBatch.fill(rawRegion.minX, rawRegion.minY, splitX - 1, barY, barColor)
	colorBatch.fill(
		rawRegion.minX, barY - rawRegion.height / 500, splitX - 1, barY,
		srgbToLinear(rgba(68, 51, 34, transitionAlpha))
	)
	colorBatch.gradient(
		rawRegion.minX, barY + 1, rawRegion.maxX, rawRegion.maxY,
		srgbToLinear(rgba(47, 33, 20, transitionAlpha)),
		srgbToLinear(rgba(88, 58, 36, transitionAlpha)),
		srgbToLinear(rgba(45, 32, 19, transitionAlpha)),
	)
	colorBatch.gradient(
		splitX + 1, rawRegion.minY, rawRegion.maxX, rawRegion.maxY,
		srgbToLinear(rgba(129, 83, 53, transitionAlpha)),
		srgbToLinear(rgba(129, 83, 53, transitionAlpha)),
		srgbToLinear(rgba(35, 21, 15, transitionAlpha)),
	)
	colorBatch.fill(
		splitX, rawRegion.minY, splitX + rawRegion.width / 500, rawRegion.maxY,
		srgbToLinear(rgba(152, 105, 61, transitionAlpha)),
	)

	val baseSlotSize = 18
	val itemScale = min((splitX - rawRegion.minX) / baseSlotSize / 14, rawRegion.height / baseSlotSize / 13)

	val inventoryContext = InventoryRenderContext(
		context, colorBatch, null, spriteBatch,
		imageBatch, lateColorBatch, simpleTextBatch, fancyTextBatch,
	)

	val playerMinX = rawRegion.minX - (fadeStrength * (splitX - rawRegion.minX)).roundToInt()
	val playerMaxX = playerMinX + (splitX - rawRegion.minX)
	val shopMinX = splitX + (fadeStrength * (rawRegion.boundX - splitX)).roundToInt()
	if (itemScale > 0) {
		renderedCharacterBars = renderCharacterBars(
			inventoryContext, inventory, context.campaign.usedPartyMembers(),
			playerMinX + 5 * itemScale, barY + 3 * itemScale, playerMaxX - 1, itemScale,
		).toTypedArray()

		val shopRegion = Rectangle(
			shopMinX,
			rawRegion.minY + rawRegion.height / 10,
			rawRegion.boundX - splitX,
			rawRegion.boundY - rawRegion.minY - rawRegion.height / 6,
		)
		renderedShopInventory = renderShopInventory(inventoryContext, itemScale, shopRegion, state)
		updateHoveredShopSlot()

		val gridOffset = 2 + 3 * itemScale + 8 * itemScale * SIMPLE_SLOT_SIZE
		val gridStartX = playerMaxX - 1 - gridOffset
		val inventoryY = rawRegion.boundY - gridOffset
		val selectedCharacter = context.campaign.allPartyMembers()[inventory.partyIndex]
		if (selectedCharacter != null) {
			val infoMaxX = min(playerMinX + 200 * itemScale, gridStartX - 2 * itemScale)
			val hoveredShopItem = if (hoveredShopInventoryIndex != -1) {
				val shopState = context.campaign.shops.get(shop)
				shop.fixedItems[hoveredShopInventoryIndex] ?: shopState.inventory[hoveredShopInventoryIndex]?.item
			} else null
			renderHoverItemProperties(
				inventoryContext, inventory, selectedCharacter.second,
				playerMinX, inventoryY, infoMaxX, rawRegion.maxY, itemScale,
				defaultHoverItem = hoveredShopItem,
			)
			renderedCharacterInventory = renderItemGrid(
				inventoryContext, selectedCharacter.second.inventory, inventory,
				gridStartX, rawRegion.boundY - gridOffset, itemScale,
			)
		}
	}

	val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
	simpleTextBatch.drawString(
		"Inventory", playerMinX + barHeight / 4, rawRegion.minY + 3 * barHeight / 4,
		barHeight / 2, upperFont, srgbToLinear(rgb(131, 81, 37)),
	)
	simpleTextBatch.drawString(
		"Shop", shopMinX + barHeight / 4, rawRegion.minY + 3 * barHeight / 4,
		barHeight / 2, upperFont, srgbToLinear(rgb(238, 203, 127)),
	)

	val simpleFont = context.bundle.getFont(context.content.fonts.basic2.index)
	simpleTextBatch.drawString(
		"Value:", shopMinX + rawRegion.height * 0.12f, rawRegion.boundY - rawRegion.height * 0.03f,
		rawRegion.height * 0.025f, simpleFont,
		srgbToLinear(rgb(207, 192, 141)),
		srgbToLinear(rgb(53, 34, 22)),
		rawRegion.height * 0.001f, TextAlignment.RIGHT,
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
	simpleTextBatch.drawShadowedString(
		hoveredItemValue.toString(), shopMinX + rawRegion.height * 0.13f,
		rawRegion.boundY - rawRegion.height * 0.025f, rawRegion.height * 0.03f, valueFont,
		MardekTextStyles.ShopUI.ITEM_VALUE, TextAlignment.LEFT,
	)

	val overlayRegion = Rectangle(playerMinX, barY, playerMaxX - playerMinX, rawRegion.maxY)
	thrashRegion = renderInventoryOverlay(
		inventoryContext, overlayRegion, itemScale, context.campaign, inventory,
		renderedCharacterBars.toList(), thrashRegion,
	)

	renderShopTradeOverlay(context, this, rawRegion)

	Pair(colorBatch, simpleTextBatch)
}
