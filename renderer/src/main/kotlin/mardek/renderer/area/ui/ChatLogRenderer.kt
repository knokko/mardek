package mardek.renderer.area.ui

import com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_add_utf16
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_clear_contents
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_guess_segment_properties
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_shape
import kotlin.use

internal fun renderChatLog(dialogueContext: DialogueRenderContext) {
	dialogueContext.run {
		val titleFont = context.bundle.getFont(context.content.fonts.large1.index)
		simpleTextBatch.drawString(
			"Chat Log", region.maxX - region.width * 0.025f, region.minY + region.height * 0.06f,
			region.height * 0.03f, titleFont,
			MardekTextStyles.Dialogue.CHAT_LOG_BASE.fill.color, TextAlignment.RIGHT,
		)

		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		var textY = region.minY + 0.125f * region.height
		val minTextX = region.minX + 0.01f * region.height

		val maxLineY = region.minY + 0.65f * region.height
		for (entry in context.campaign.chatLog.reversed()) {
			if (textY > maxLineY) break

			val heightA = 0.0175f * region.height
			val lineSpacing = 0.025f * region.height
			var nameStyle = MardekTextStyles.Dialogue.CHAT_LOG_BASE
			val customNameColor = entry.speakerElement?.chatLogColor
			if (customNameColor != null) nameStyle = nameStyle.withDifferentFillColor(customNameColor)
			simpleTextBatch.drawString(
				entry.speaker, minTextX, textY,
				heightA, baseFont, nameStyle, TextAlignment.LEFT,
			)

			var textX = minTextX
			run {
				hb_buffer_clear_contents(simpleTextBatch.cache.hbBuffer)

				MemoryStack.stackPush().use { stack ->
					val textBytes = stack.UTF16(entry.speaker, false)
					hb_buffer_add_utf16(
						simpleTextBatch.cache.hbBuffer, textBytes,
						0, textBytes.capacity()
					)
				}

				hb_buffer_guess_segment_properties(simpleTextBatch.cache.hbBuffer)
				hb_shape(baseFont.hbFont, simpleTextBatch.cache.hbBuffer, null)

				val glyphOffsets = assertHbSuccess(
					hb_buffer_get_glyph_positions(simpleTextBatch.cache.hbBuffer),
					"buffer_get_glyph_positions"
				)!!

				for (offset in glyphOffsets) {
					textX += baseFont.getGlyphAdvanceX(offset, heightA)
				}
			}

			textY = renderDialogueLines(
				": ${entry.text}", entry.text.length.toFloat() + 2f,
				minTextX, textX, region.maxX - 0.05f * region.height, textY, maxLineY,
				heightA, lineSpacing, lineSpacing,
				simpleTextBatch, baseFont, MardekTextStyles.Dialogue.CHAT_LOG_BASE,
				MardekTextStyles.Dialogue.CHAT_LOG_BOLD, null,
			)
			textY += 0.05f * region.height
		}
	}
}
