package mardek.renderer.actions

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.renderer.RenderContext
import mardek.renderer.area.ui.storage.renderItemStorage
import mardek.renderer.save.renderSaveSelectionModal
import mardek.renderer.util.renderButton
import mardek.state.ingame.actions.EndOfChapterState
import mardek.state.util.Rectangle

internal fun renderEndOfChapter(
	context: RenderContext, region: Rectangle, chapter: Int, state: EndOfChapterState
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	return if (state.itemStorage != null) {
		renderItemStorage(context, state.itemStorage!!, region)
	} else if (state.saveSelectionState != null) {
		val basicFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val fatFont = context.bundle.getFont(context.content.fonts.fat.index)
		val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
		renderSaveSelectionModal(
			context, basicFont, fatFont, upperFont,
			state.saveSelectionState!!, true, region,
		)
	} else {
		renderEndOfChapterBase(context, region, chapter, state)
	}
}

private fun renderEndOfChapterBase(
	context: RenderContext, region: Rectangle, chapter: Int, state: EndOfChapterState
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val colorBatch = context.addColorBatch(100)

	val lineY = region.minY + region.height / 5
	colorBatch.fill(
		region.minX + region.width / 7, lineY, region.minX + 3 * region.width / 5, lineY,
		MardekTextStyles.EndOfChapter.TITLE.fill.color,
	)

	val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
	val simpleTextBatch = context.addTextBatch(250)
	simpleTextBatch.drawString(
		"End of Chapter $chapter", region.minX + 0.2f * region.width,
		region.minY + 0.17f * region.height, 0.038f * region.height, upperFont,
		MardekTextStyles.EndOfChapter.TITLE, TextAlignment.LEFT,
	)

	val infoX = region.minX + 0.25f * region.width
	val infoY = region.minY + 0.27f * region.height
	val infoHeight = 0.022f * region.height
	val infoFont = context.bundle.getFont(context.content.fonts.basic2.index)
	simpleTextBatch.drawString(
		"This is the end of Chapter $chapter.", infoX, infoY, infoHeight, infoFont,
		MardekTextStyles.EndOfChapter.INFO, TextAlignment.LEFT,
	)
	simpleTextBatch.drawString(
		"Do you want to record your progress?", infoX, infoY + 0.06f * region.height,
		infoHeight, infoFont, MardekTextStyles.EndOfChapter.INFO, TextAlignment.LEFT,
	)

	val ovalBatch = context.addOvalBatch(20)
	val fancyTextBatch = context.addFancyTextBatch(100)

	fun renderButton(text: String, buttonIndex: Int): Rectangle {
		val buttonRegion = Rectangle(
			region.minX + 5 * region.width / 9,
			region.minY + 5 * region.height / 9 + buttonIndex * region.height / 9,
			region.height / 3, region.height / 14,
		)
		renderButton(
			colorBatch, ovalBatch, fancyTextBatch, upperFont, true, text, true,
			state.selectedButtonIndex == buttonIndex, false, buttonRegion,
			region.height / 200, buttonRegion.minX + region.height / 40,
			buttonRegion.maxY - region.height / 55, region.height / 27,
		)
		return buttonRegion
	}

	state.saveButton = renderButton("Save", 0)
	state.itemStorageButton = renderButton("Item Storage", 1)
	// TODO CHAP2 state.continueButton = renderButton("Continue", 2)

	return Pair(colorBatch, simpleTextBatch)
}
