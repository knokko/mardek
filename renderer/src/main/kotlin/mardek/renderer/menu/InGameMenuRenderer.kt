package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.menu.inventory.renderInventory
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.EncyclopediaTab
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.inventory.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.ingame.menu.PartyTab
import mardek.state.ingame.menu.QuestsTab
import mardek.state.ingame.menu.SkillsTab
import mardek.state.ingame.menu.StatusTab
import mardek.state.ingame.menu.SettingsTab
import mardek.state.util.Rectangle
import mardek.content.util.Time
import kotlin.math.roundToInt

internal fun renderInGameMenu(
	context: RenderContext, region: Rectangle, regionShift: Float, menu: InGameMenuState, state: CampaignState
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val shiftOpacity = 1f - regionShift
	val shiftAlpha = (255f * shiftOpacity).roundToInt()

	val colorBatch = context.addColorBatch(10_000) // The map tab uses a lot of colors
	val ovalBatch = context.addOvalBatch(1000) // The encyclopedia may draw a lot of ovals
	val spriteBatch = context.addKim3Batch(1000) // The inventory tab could use a lot of sprites
	val areaSpriteBatch = context.addAreaSpriteBatch(10, region) // Only used for encyclopedia
	val imageBatch = context.addImageBatch(500) // The encyclopedia may draw a lot of element icons
	val animationBatch = context.addAnimationPartBatch(200) // Only needed for the encyclopedia & party tabs
	val lateColorBatch = context.addColorBatch(20) // Only needed for inventory tooltips
	val simpleTextBatch = context.addTextBatch(2500) // The encyclopedia tab has quite some text
	val fancyTextBatch = context.addFancyTextBatch(500) // For rendering "MASTERED"
	val barColor = srgbToLinear(rgba(24, 14, 10, shiftAlpha))
	val barHeight = determineBarHeight(region)

	val sectionsWidth = determineSectionsWidth(region)
	if (menu.currentTab.shouldShowLowerBar()) {
		colorBatch.fill(
			region.minX, region.maxY - barHeight,
			region.maxX, region.maxY, barColor
		)
	} else if (menu.currentTab.shouldShowLowerBarClock()) {
		colorBatch.fillUnaligned(
			region.maxX - sectionsWidth, region.maxY + 1,
			region.maxX + 1, region.maxY + 1,
			region.maxX + 1, region.maxY - barHeight,
			region.maxX + barHeight - sectionsWidth, region.maxY - barHeight, barColor
		)
	}
	colorBatch.fill(
		region.minX, region.minY,
		region.maxX, region.minY + barHeight, barColor
	)
	colorBatch.fill(
		region.minX, region.minY + barHeight - region.height / 500, region.maxX, region.minY + barHeight,
		srgbToLinear(rgba(68, 51, 34, shiftAlpha))
	)

	simpleTextBatch.drawString(
		menu.currentTab.getText(), region.minX + barHeight / 4, region.minY + 3 * barHeight / 4,
		barHeight / 2, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgba(131, 81, 37, shiftAlpha))
	)

	if (menu.currentTab.shouldShowLowerBarClock()) {
		val clockSize = 0.9f * barHeight
		val clockMargin = (barHeight - clockSize) / 2
		imageBatch.simple(
			region.maxX - clockMargin - clockSize, region.maxY - clockMargin - clockSize,
			region.maxX - clockMargin, region.maxY - clockMargin, context.content.ui.clock.index
		)

		val totalSeconds = context.timing.elapsedTimeSince(Time.ZERO).inWholeSeconds
		fun minutesOrHours(raw: Long) = if (raw < 10) "0$raw" else raw.toString()
		simpleTextBatch.drawString(
			"${totalSeconds / 3600}:${minutesOrHours((totalSeconds % 3600) / 60)}:${minutesOrHours(totalSeconds % 60)}",
			region.maxX - clockSize - 4f * clockMargin, region.maxY - barHeight * 0.22f,
			barHeight * 0.5f, context.bundle.getFont(context.content.fonts.large1.index),
			srgbToLinear(rgba(238, 203, 127, shiftAlpha)), TextAlignment.RIGHT
		)
	}

	val menuContext = MenuRenderContext(
		context, colorBatch, ovalBatch, areaSpriteBatch, imageBatch, spriteBatch,
		animationBatch, lateColorBatch, simpleTextBatch, fancyTextBatch, menu, state
	)
	if (!menu.currentTab.inside && menu.currentTab.shouldShowSectionList()) {
		renderInGameMenuSectionList(menuContext, Rectangle(
			region.maxX - (sectionsWidth * (1f - regionShift)).roundToInt(),
			region.minY + barHeight, sectionsWidth, region.height - 2 * barHeight
		))
	}

	val contentWidth = region.width - sectionsWidth
	val submenuRectangleWithoutLowerBar = Rectangle(
		(region.minX - regionShift * contentWidth).roundToInt(),
		region.minY + barHeight, region.width - sectionsWidth, region.height - 2 * barHeight
	)
	val submenuRectangleWithLowerBar = Rectangle(
		(region.minX - regionShift * contentWidth).roundToInt(),
		region.minY + barHeight, region.width - sectionsWidth, region.height - barHeight
	)
	if (menu.currentTab is PartyTab) renderPartyTab(menuContext, submenuRectangleWithoutLowerBar)
	if (menu.currentTab is SkillsTab) renderSkillsTab(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is InventoryTab) renderInventory(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is MapTab) renderAreaMap(menuContext, submenuRectangleWithoutLowerBar)
	if (menu.currentTab is QuestsTab) renderQuestsTab(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is StatusTab) renderStatusTab(menuContext, submenuRectangleWithLowerBar)
	if (menu.currentTab is EncyclopediaTab) renderEncyclopediaTab(
		menuContext, submenuRectangleWithLowerBar, region
	)
	if (menu.currentTab is SettingsTab) renderSettingsTab(menuContext, submenuRectangleWithLowerBar)

	return Pair(colorBatch, simpleTextBatch)
}

private fun determineSectionsWidth(region: Rectangle) = region.height / 3

private fun determineBarHeight(region: Rectangle) = region.height / 12

internal fun determineSectionRenderRegion(region: Rectangle) = Rectangle(
	region.maxX - determineSectionsWidth(region), region.minY + determineBarHeight(region),
	determineSectionsWidth(region), region.height - 2 * determineBarHeight(region)
)
