package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.menu.inventory.renderInventory
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.ingame.menu.QuestsTab
import mardek.state.ingame.menu.SkillsTab
import mardek.state.ingame.menu.VideoSettingsTab
import mardek.state.util.Rectangle

internal fun renderInGameMenu(
	context: RenderContext, region: Rectangle, menu: InGameMenuState, state: CampaignState
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val colorBatch = context.addColorBatch(10_000) // The map tab uses a lot of colors
	val ovalBatch = context.addOvalBatch(100)
	val spriteBatch = context.addKim3Batch(1000) // The inventory tab could use a lot of sprites
	val imageBatch = context.addImageBatch(100)
	val textBatch = context.addFancyTextBatch(2000)
	val barColor = srgbToLinear(rgb(24, 14, 10))
	val barHeight = determineBarHeight(region)

	val selectionWidth = determineSelectionWidth(region)
	if (menu.currentTab.shouldShowLowerBar()) {
		colorBatch.fill(
			region.minX, region.maxY - barHeight,
			region.maxX, region.maxY, barColor
		)
	} else {
		colorBatch.fillUnaligned(
			region.maxX - selectionWidth, region.maxY + 1,
			region.maxX + 1, region.maxY + 1,
			region.maxX + 1, region.maxY - barHeight,
			region.maxX + barHeight - selectionWidth, region.maxY - barHeight, barColor
		)
	}
	colorBatch.fill(
		region.minX, region.minY,
		region.maxX, region.minY + barHeight, barColor
	)
	colorBatch.fill(
		region.minX, region.minY + barHeight - region.height / 500, region.maxX, region.minY + barHeight,
		srgbToLinear(rgb(68, 51, 34))
	)

	textBatch.drawString(
		menu.currentTab.getText(), region.minX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgb(131, 81, 37))
	)

	val clockSize = 0.9f * barHeight
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

	val menuContext = MenuRenderContext(context, colorBatch, ovalBatch, imageBatch, spriteBatch, textBatch, menu, state)
	if (!menu.currentTab.inside) {
		renderInGameMenuSectionList(menuContext, Rectangle(
			region.maxX - selectionWidth, region.minY + barHeight, selectionWidth, region.height - 2 * barHeight
		))
	}

	val submenuRectangleWithoutLowerBar = Rectangle(
		region.minX, region.minY + barHeight, region.width - selectionWidth, region.height - 2 * barHeight
	)
	val submenuRectangleWithLowerBar = Rectangle(
		region.minX, region.minY + barHeight, region.width - selectionWidth, region.height - barHeight
	)
	if (menu.currentTab is SkillsTab) renderSkillsTab(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is InventoryTab) renderInventory(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is MapTab) renderAreaMap(menuContext, submenuRectangleWithoutLowerBar)
	if (menu.currentTab is QuestsTab) renderQuestsTab(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is VideoSettingsTab) renderVideoSettingsTab(menuContext, submenuRectangleWithLowerBar)

	return Pair(colorBatch, textBatch)
}

private fun determineSelectionWidth(region: Rectangle) = region.height / 3

private fun determineBarHeight(region: Rectangle) = region.height / 12

internal fun determineSectionRenderRegion(region: Rectangle) = Rectangle(
	region.maxX - determineSelectionWidth(region), region.minY + determineBarHeight(region),
	determineSelectionWidth(region), region.height - 2 * determineBarHeight(region)
)
