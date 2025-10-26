package mardek.renderer.save

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.renderer.RenderContext
import mardek.state.saves.SaveSelectionState
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.min

internal fun renderSaveSelectionModal(
	context: RenderContext, basicFont: Vk2dFont, fatFont: Vk2dFont, upperFont: Vk2dFont,
	state: SaveSelectionState, isSaving: Boolean, region: Rectangle,
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val colorBatch = context.addColorBatch(200)
	val partBatch = context.addAnimationPartBatch(1500)
	val imageBatch = context.addImageBatch(50)
	val textBatch = context.addTextBatch(2000)

	val upperLineY = region.minY + region.height / 12
	colorBatch.fill(
		region.minX, region.minY, region.maxX, upperLineY - 1,
		srgbToLinear(rgb(24, 14, 10)),
	)
	colorBatch.fill(
		region.minX, upperLineY, region.maxX, upperLineY,
		srgbToLinear(rgb(68, 51, 34)),
	)

	val leftColor = srgbToLinear(rgba(34, 21, 16, 200))
	val rightColor = srgbToLinear(rgba(80, 52, 35, 200))
	colorBatch.gradient(
		region.minX, upperLineY + 1, region.maxX, region.maxY,
		leftColor, rightColor, leftColor,
	)

	textBatch.drawString(
		if (isSaving) "Save" else "Load", region.minX + region.width * 0.05f,
		region.minY + 0.065f * region.height, 0.05f * region.height, upperFont,
		srgbToLinear(rgb(131, 81, 17)),
	)

	val textColor = srgbToLinear(rgb(238, 203, 127))
	val campaignName = state.getSelectedCampaign()
	run {
		textBatch.drawString(
			campaignName, region.minX + 0.5f * region.width, region.minY + 0.06f * region.height,
			0.04f * region.height, basicFont, textColor, TextAlignment.CENTERED,
		)

		if (state.selectableCampaigns.size > 1) {
			val arrowHead = context.content.ui.arrowHead
			val scale = 0.06f * region.height / arrowHead.height
			if (state.selectedCampaignIndex > 0) {
				imageBatch.rotated(
					region.minX + 0.35f * region.width,
					region.minY + region.height / 24f,
					180f, scale, arrowHead.index, 0, -1,
				)
			}
			if (state.selectedCampaignIndex + 1 < state.selectableCampaigns.size) {
				imageBatch.rotated(
					region.minX + 0.65f * region.width,
					region.minY + region.height / 24f,
					0f, scale, arrowHead.index, 0, -1,
				)
			}
		}
	}

	val saveHeight = region.height / 11
	val saveWidth = saveHeight * SAVE_FILE_ASPECT_RATIO
	val saveOffsetX = (region.width - saveWidth) / 2
	val saveMinX = region.minX + saveOffsetX
	val saveMinY = region.minY + region.height / 10
	val saveMaxX = region.maxX - saveOffsetX

	textBatch.drawString(
		"Use Q to cancel", saveMinX.toFloat(), region.maxY - 0.015f * region.height,
		0.025f * region.height, basicFont, textColor,
	)
	textBatch.drawString(
		"Use E to confirm", saveMaxX.toFloat(), region.maxY - 0.015f * region.height,
		0.025f * region.height, basicFont, textColor, TextAlignment.RIGHT,
	)

	var firstIndex = state.selectedFileIndex - 3
	firstIndex = if (isSaving) max(-1, firstIndex)
	else max(0, firstIndex)

	if (state.selectableFiles.size > 7) {
		firstIndex = min(firstIndex, state.selectableFiles.size - 7)
	}

	for (renderSlot in 0 until 7) {
		val saveIndex = firstIndex + renderSlot
		val saveRegion = Rectangle(saveMinX, saveMinY + renderSlot * region.height / 8, saveWidth, saveHeight)
		if (saveIndex < state.selectableFiles.size) {
			val saveFile = if (saveIndex == -1) null else state.selectableFiles[saveIndex]
			val isSelected = saveIndex == state.selectedFileIndex
			renderSaveFile(
				colorBatch, imageBatch, partBatch, textBatch, fatFont, context.content, saveFile,
				isSelected = isSelected, isGray = false, saveIndex, saveRegion,
			)
		} else {
			renderSaveFile(
				colorBatch, imageBatch, partBatch, textBatch, fatFont, context.content, null,
				isSelected = false, isGray = true, -1, saveRegion,
			)
		}
	}

	return Pair(colorBatch, textBatch)
}
