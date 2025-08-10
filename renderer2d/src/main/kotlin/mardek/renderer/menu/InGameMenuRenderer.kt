package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.menu.inventory.renderInventory
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.util.Rectangle

internal fun renderInGameMenu(
	context: RenderContext, region: Rectangle, menu: InGameMenuState, state: CampaignState
): Vk2dColorBatch {
	val colorBatch1 = context.addColorBatch(10_000) // The map tab uses a lot of colors
	val spriteBatch = context.addKim3Batch(500) // TODO Figure out right amount
	val imageBatch = context.addImageBatch(500) // TODO Figure out right amount
	val textBatch = context.addFancyTextBatch(500) // TODO Figure out right amount
	val barColor = srgbToLinear(rgb(24, 14, 10))
	val barHeight = region.height / 12

	val leftColor = srgbToLinear(rgba(54, 37, 21, 179))
	val rightColor = srgbToLinear(rgba(132, 84, 53, 179))
	colorBatch1.gradient(
		region.minX, region.minY + barHeight, region.maxX,
		if (menu.currentTab.shouldShowLowerBar()) region.maxY - barHeight else region.maxY,
		leftColor, rightColor, leftColor
	)

	val selectionWidth = region.height / 3
	if (menu.currentTab.shouldShowLowerBar()) {
		colorBatch1.fill(
			region.minX, region.maxY - barHeight,
			region.maxX, region.maxY, barColor
		)
	} else {
		colorBatch1.fillUnaligned(
			region.maxX - selectionWidth, region.maxY + 1,
			region.maxX + 1, region.maxY + 1,
			region.maxX + 1, region.maxY - barHeight,
			region.maxX + barHeight - selectionWidth, region.maxY - barHeight, barColor
		)
	}
	colorBatch1.fill(
		region.minX, region.minY,
		region.maxX, region.minY + barHeight, barColor
	)

	textBatch.drawString(
		menu.currentTab.getText(), region.minX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgb(131, 81, 37))
	)

	val clockSize = 9 * barHeight / 10
	val clockMargin = (barHeight - clockSize) / 2
	imageBatch.simple(
		region.maxX - clockMargin - clockSize, region.maxY - clockMargin - clockSize,
		region.maxX - clockMargin, region.maxY - clockMargin, context.content.ui.clock.index
	)

	val totalSeconds = state.totalTime.inWholeSeconds
	fun minutesOrHours(raw: Long) = if (raw < 10) "0$raw" else raw.toString()
	textBatch.drawString(
		"${totalSeconds / 3600}:${minutesOrHours((totalSeconds % 3600) / 60)}:${minutesOrHours(totalSeconds % 60)}",
		region.maxX - clockSize - 4f * clockMargin, region.maxY - barHeight * 0.22f,
		barHeight * 0.5f, context.bundle.getFont(context.content.fonts.large1.index),
		srgbToLinear(rgb(238, 203, 127)), TextAlignment.RIGHT
	)

	val menuContext = MenuRenderContext(context, colorBatch1, imageBatch, spriteBatch, textBatch, menu, state)
	renderInGameMenuSectionList(menuContext, Rectangle(
		region.maxX - selectionWidth, region.minY + barHeight, selectionWidth, region.height - 2 * barHeight
	))

	val submenuRectangleWithoutLowerBar = Rectangle(
		region.minX, region.minY + barHeight, region.width - selectionWidth, region.height - 2 * barHeight
	)
	val submenuRectangleWithLowerBar = Rectangle(
		region.minX, region.minY + barHeight, region.width - selectionWidth, region.height - barHeight
	)
	if (menu.currentTab is InventoryTab) renderInventory(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is MapTab) renderAreaMap(menuContext, submenuRectangleWithoutLowerBar)

	return colorBatch1
}
