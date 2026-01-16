package mardek.renderer.menu.inventory

import mardek.renderer.menu.MenuRenderContext
import mardek.state.ingame.menu.inventory.InventoryTab
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.min

private const val CHARACTER_BAR_WIDTH = 135

private const val BASE_WIDTH = 3 + 6 * EQUIPMENT_SLOT_SIZE + CHARACTER_BAR_WIDTH
private const val BASE_HEIGHT = 3 + 4 * CHARACTER_BAR_HEIGHT + 8 * SIMPLE_SLOT_SIZE

internal fun renderInventory(menuContext: MenuRenderContext, region: Rectangle) {
	if (region.width < 50) return
	val scale = max(1, min(region.width / BASE_WIDTH, region.height / BASE_HEIGHT))
	val gridOffset = 2 + 3 * scale + 8 * scale * SIMPLE_SLOT_SIZE

	val gridStartX = region.boundX - gridOffset
	menuContext.run {
		val tab = menu.currentTab as InventoryTab
		val inventoryContext = InventoryRenderContext(context, colorBatch, spriteBatch, imageBatch, lateColorBatch, textBatch)
		val (_, characterState) = state.allPartyMembers()[tab.interaction.partyIndex]!!
		tab.equipmentRenderInfo = renderCharacterBars(
			inventoryContext, tab.interaction, state.usedPartyMembers(),
			region.minX + 5 * scale, region.minY + 3 * scale, region.maxX, scale,
		)
		if (gridStartX >= 30 * scale) {
			val startY = region.boundY - gridOffset
			val maxX = min(200 * scale, gridStartX - 2 * scale)
			renderHoverItemProperties(
				inventoryContext, tab.interaction, characterState,
				region.minX, startY, maxX, region.maxY, scale,
			)
		}
		tab.thrashRegion = renderInventoryOverlay(
			inventoryContext, region, scale, state, tab.interaction,
			tab.equipmentRenderInfo, tab.thrashRegion,
		)
		tab.gridRenderInfo = renderItemGrid(
			inventoryContext, characterState.inventory, tab.interaction,
			gridStartX, region.boundY - gridOffset, scale,
		)
	}
}
