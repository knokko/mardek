package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.area.AreaRenderContext
import mardek.state.ingame.actions.AreaActionsState

internal fun renderChatLog(areaContext: AreaRenderContext, actions: AreaActionsState) {
	areaContext.run {
		val baseColor = srgbToLinear(rgb(186, 146, 77))
		val boldColor = srgbToLinear(rgb(253, 218, 116))
		val titleFont = context.bundle.getFont(context.content.fonts.large1.index)
		textBatch.drawString(
			"Chat Log", region.maxX - region.width * 0.025f, region.minY + region.height * 0.06f,
			region.height * 0.03f, titleFont, baseColor, TextAlignment.RIGHT,
		)

		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		var textY = region.minY + 0.125f * region.height
		val minTextX = region.minX + 0.01f * region.height

		val maxLineY = region.minY + 0.65f * region.height
		for (entry in actions.chatLog.reversed()) {
			if (textY > maxLineY) break

			val heightA = 0.0175f * region.height
			val lineSpacing = 0.025f * region.height
			val nameColor = entry.speakerElement?.color ?: baseColor
			textBatch.drawString(
				entry.speaker, minTextX, textY,
				heightA, baseFont, nameColor
			)

			var textX = minTextX + 0.003f * region.height
			for (textChar in entry.speaker) {
				val glyph = baseFont.getGlyphForChar(textChar.code)
				textX += heightA * baseFont.getGlyphAdvance(glyph)
			}

			textY = renderDialogueLines(
				": ${entry.text}", entry.text.length.toFloat() + 2f,
				minTextX, textX, region.maxX - 0.05f * region.height, textY, maxLineY,
				heightA, lineSpacing, lineSpacing,
				textBatch, baseFont, baseFont, baseColor, boldColor,
				0, 0f
			)
			textY += 0.05f * region.height
		}
	}
}
