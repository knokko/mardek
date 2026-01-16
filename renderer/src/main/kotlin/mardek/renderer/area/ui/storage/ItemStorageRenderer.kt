package mardek.renderer.area.ui.storage

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.menu.inventory.InventoryRenderContext
import mardek.renderer.menu.inventory.renderCharacterBars
import mardek.renderer.menu.inventory.renderHoverItemProperties
import mardek.renderer.menu.inventory.renderInventoryOverlay
import mardek.renderer.menu.inventory.renderItemGrid
import mardek.state.UsedPartyMember
import mardek.state.ingame.actions.ItemStorageInteractionState
import mardek.state.util.Rectangle
import kotlin.math.min

internal fun renderItemStorage(
	context: RenderContext, state: ItemStorageInteractionState, region: Rectangle
) = state.run {
	val colorBatch = context.addColorBatch(5000) // There are quite some slots to render...
	val spriteBatch = context.addKim3Batch(200) // Character slots, item slots, equipment slots...
	val imageBatch = context.addImageBatch(50) // Only a couple of icons
	val lateColorBatch = context.addColorBatch(2) // Only for equipment slot name tooltips
	val textBatch = context.addFancyTextBatch(5000) // Item descriptions can be long

	val splitX = region.minX + 7 * region.width / 10
	val barHeight = region.height / 12
	val barY = region.minY + barHeight

	val barColor = srgbToLinear(rgb(24, 14, 10))
	colorBatch.fill(
		region.minX, region.minY,
		region.maxX, barY, barColor
	)
	colorBatch.fill(
		region.minX, barY - region.height / 500, region.maxX, barY,
		srgbToLinear(rgb(68, 51, 34))
	)
	colorBatch.gradient(
		region.minX, barY + 1, region.maxX, region.maxY,
		srgbToLinear(rgb(40, 27, 19)),
		srgbToLinear(rgb(83, 55, 36)),
		srgbToLinear(rgb(32, 20, 14)),
	)

	val numItemColumns = 15
	val numItemRows = 13

	val startStorageX = region.minX + region.width / 50
	val boundItemsX = splitX - region.width / 70
	val startStorageY = barY + region.height / 40
	val verticalItemsSpace = 9 * region.height / 10
	val baseSlotSize = 18
	val itemScale = min(
		(boundItemsX - startStorageX) / baseSlotSize / numItemColumns,
		verticalItemsSpace / baseSlotSize / numItemRows,
	)

	val inventoryContext = InventoryRenderContext(
		context, colorBatch, spriteBatch, imageBatch, lateColorBatch, textBatch
	)

	val storageRegion = Rectangle(
		startStorageX, startStorageY,
		boundItemsX - startStorageX, 3 * baseSlotSize * itemScale,
	)
	renderedStorageInventory = renderItemStorageInventory(inventoryContext, itemScale, storageRegion, state)
	updateHoveredStorageSlot(context.campaign.itemStorage)

	if (itemScale >= 1) {
		val fullSlotSize = baseSlotSize * itemScale

		if (selectedCharacter != null) {
			val (character, characterState) = selectedCharacter!!
			renderedCharacterInventory = renderItemGrid(
				inventoryContext, characterState.inventory, inventory,
				startStorageX + 7 * fullSlotSize, startStorageY + 5 * fullSlotSize, itemScale,
			)
			inventory.partyIndex = -1
			renderedCharacterBar = renderCharacterBars(
				inventoryContext, inventory,
				listOf(UsedPartyMember(0, character, characterState)),
				startStorageX, startStorageY + 10 * fullSlotSize / 3,
				boundItemsX, itemScale,
			).iterator().next()
			renderHoverItemProperties(
				inventoryContext, inventory, characterState,
				region.minX, startStorageY + 5 * fullSlotSize,
				startStorageX + 7 * fullSlotSize - region.width / 100, region.maxY, itemScale,
			)
		}
	}

	val startCharactersX = splitX + region.width / 70
	val startCharactersY = barY + region.height / 50
	val baseCharacterSpacing = 22
	val minCharacterColumns = 3
	val minCharacterRows = 8
	val characterScale = min(
		(region.boundX - startCharactersX) / baseCharacterSpacing / minCharacterColumns,
		(region.boundY - startCharactersY) / baseCharacterSpacing / minCharacterRows,
	)
	if (characterScale >= 1) {
		val charactersRegion = Rectangle(
			startCharactersX, startCharactersY,
			region.boundX - startCharactersX, region.boundY - startCharactersY,
		)
		renderedCharacters = renderItemStorageCharacterSlots(inventoryContext, characterScale, charactersRegion, inventory)
	}

	textBatch.drawString(
		"Item Storage", region.minX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgb(131, 81, 37))
	)

	textBatch.drawString(
		"Page ${storagePage + 1}", region.minX + 0.6f * region.height, region.minY + 0.7f * barHeight,
		0.025f * region.height, context.bundle.getFont(context.content.fonts.basic2.index),
		srgbToLinear(rgb(207, 192, 141)),
	)

	val arrow = context.content.ui.arrowHead
	val arrowScale = 0.03f * region.height / arrow.height
	if (storagePage > 0) {
		imageBatch.rotated(
			region.minX + 0.575f * region.height, region.minY + 0.3f * barHeight,
			90f, arrowScale, arrow.index, 0, -1,
		)
	}

	if (canScrollToNextPage(context.campaign.itemStorage)) {
		imageBatch.rotated(
			region.minX + 0.575f * region.height, region.minY + 0.7f * barHeight,
			270f, arrowScale, arrow.index, 0, -1,
		)
	}

	textBatch.drawString(
		"Allies", region.maxX - barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgb(131, 81, 37)), TextAlignment.RIGHT,
	)

	val overlayRegion = Rectangle(region.minX, barY, region.maxX, region.maxY)
	thrashRegion = renderInventoryOverlay(
		inventoryContext, overlayRegion, itemScale, context.campaign, inventory,
		if (renderedCharacterBar != null) listOf(renderedCharacterBar!!) else emptyList(),
		thrashRegion,
	)

	Pair(colorBatch, textBatch)
}
