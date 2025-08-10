package mardek.renderer.menu.inventory

import mardek.renderer.menu.MenuRenderContext
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
	renderInventoryGrid(menuContext, gridStartX, region.boundY - gridOffset, scale)
	renderCharacterBars(menuContext, region.minX + 5 * scale, region.minY + 3 * scale, region.maxX, scale)
	if (gridStartX >= 30 * scale) {
		val startY = region.boundY - gridOffset
		val maxX = min(200 * scale, gridStartX - 2 * scale)
		renderHoverItemProperties(menuContext, region.minX, startY, maxX, region.maxY, scale)
	}
}
