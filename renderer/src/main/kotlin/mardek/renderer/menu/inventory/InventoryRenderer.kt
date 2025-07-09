package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
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
	menuContext.run {
		val goldX = (region.minX + region.maxX) * 2 / 3
		spriteBatch.simple(
			goldX, region.minY - 20 * scale,
			scale, context.content.ui.goldIcon.index,
		)
		textBatch.drawShadowedString(
			state.gold.toString(), goldX + 20f * scale, region.minY - 6f * scale,
			10f * scale, context.bundle.getFont(context.content.fonts.large1.index),
			srgbToLinear(rgb(255, 225, 124)), 0, 0f,
			srgbToLinear(rgba(83, 66, 50, 100)), 1.5f * scale,
			1.5f * scale, TextAlignment.LEFT,
		)
	}
}
