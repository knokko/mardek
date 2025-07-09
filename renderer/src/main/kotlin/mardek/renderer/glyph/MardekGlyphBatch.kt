package mardek.renderer.glyph

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import com.github.knokko.vk2d.text.Vk2dTextBuffer

class MardekGlyphBatch(
	pipeline: MardekGlyphPipeline,
	frame: Vk2dRenderStage,
	initialCapacity: Int,
	textBuffer: Vk2dTextBuffer,
	perFrameDescriptorSet: Long,
): Vk2dGlyphBatch(pipeline, frame, initialCapacity, textBuffer, perFrameDescriptorSet) {

	override fun glyphAt(
		baseX: Float,
		baseY: Float,
		font: Vk2dFont,
		heightA: Float,
		glyph: Int,
		fillColor: Int,
		strokeColor: Int,
		strokeWidth: Float
	) {
		val oldGlyphInfo = vertices.last().vertexData[1]
		val oldPosition = oldGlyphInfo.position()
		super.glyphAt(baseX, baseY, font, heightA, glyph, fillColor, strokeColor, strokeWidth)
		val glyphInfo = vertices.last().vertexData[1]
		if (oldGlyphInfo !== glyphInfo || oldPosition != glyphInfo.position()) {

			// GlyphInfo.yInfoAndStrokeWidth
			glyphInfo.putFloat(glyphInfo.position() - 3 * 4, baseY)
			glyphInfo.putFloat(glyphInfo.position() - 2 * 4, heightA)

			// GlyphInfo.fillColors
			glyphInfo.putInt(0).putInt(0).putInt(0).putInt(0)

			// GlyphInfo.fillDistances
			val farAway = 123456f
			glyphInfo.putFloat(farAway).putFloat(farAway).putFloat(farAway).putFloat(farAway)

			// GlyphInfo.borderColors
			glyphInfo.putInt(0).putInt(0).putInt(0).putInt(0)

			// GlyphInfo.borderDistances
			glyphInfo.putFloat(farAway).putFloat(farAway).putFloat(farAway).putFloat(farAway)
		}
	}

	fun fancyGlyphAt(
		baseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, glyph: Int, firstColor: Int,
		strokeColor: Int, strokeWidth: Float,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
		borderColor0: Int, borderColor1: Int, borderColor2: Int, borderColor3: Int,
		borderDistance0: Float, borderDistance1: Float, borderDistance2: Float, borderDistance3: Float,
	) {
		val oldGlyphInfo = vertices.last().vertexData[1]
		val oldPosition = oldGlyphInfo.position()
		super.glyphAt(baseX, baseY, font, heightA, glyph, firstColor, strokeColor, strokeWidth)
		val glyphInfo = vertices.last().vertexData[1]
		if (oldGlyphInfo !== glyphInfo || oldPosition != glyphInfo.position()) {

			// GlyphInfo.yInfoAndStrokeWidth
			glyphInfo.putFloat(glyphInfo.position() - 3 * 4, baseY)
			glyphInfo.putFloat(glyphInfo.position() - 2 * 4, heightA)

			// GlyphInfo.fillColors
			glyphInfo.putInt(color0).putInt(color1).putInt(color2).putInt(color3)

			// GlyphInfo.fillDistances
			glyphInfo.putFloat(distance0).putFloat(distance1).putFloat(distance2).putFloat(distance3)

			// GlyphInfo.borderColors
			glyphInfo.putInt(borderColor0).putInt(borderColor1).putInt(borderColor2).putInt(borderColor3)

			// GlyphInfo.borderDistances
			glyphInfo.putFloat(borderDistance0).putFloat(borderDistance1).putFloat(10f * borderDistance2).putFloat(20f * borderDistance3)
		}
	}

	fun drawFancyBorderedString(
		text: String, rawBaseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, fillColor: Int, strokeColor: Int, strokeWidth: Float, rawAlignment: TextAlignment,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
		borderColor0: Int, borderColor1: Int, borderColor2: Int, borderColor3: Int,
		borderDistance0: Float, borderDistance1: Float, borderDistance2: Float, borderDistance3: Float,
	) {
		var baseX = rawBaseX
		var alignment = rawAlignment
		if (alignment == TextAlignment.DEFAULT) alignment = TextAlignment.LEFT
		if (alignment == TextAlignment.REVERSED) alignment = TextAlignment.RIGHT

		if (alignment != TextAlignment.LEFT) {
			var width = 0f
			for (nextCharacter in text.chars()) {
				if (nextCharacter == '\t'.code) {
					width += 4f * heightA * font.whitespaceAdvance
				} else {
					val glyph = font.getGlyphForChar(nextCharacter)
					width += heightA * font.getGlyphAdvance(glyph)
				}
			}
			if (alignment == TextAlignment.CENTERED) baseX -= 0.5f * width
			if (alignment == TextAlignment.RIGHT) baseX -= width
		}

		var nextBaseX = baseX
		for (nextCharacter in text.chars()) {
			if (nextCharacter == '\t'.code) {
				nextBaseX += 4f * heightA * font.whitespaceAdvance
				continue
			}

			val glyph = font.getGlyphForChar(nextCharacter)
			fancyGlyphAt(
				nextBaseX, baseY, heightA,
				font, glyph, fillColor,
				strokeColor, strokeWidth,
				color0, color1, color2, color3,
				distance0, distance1, distance2, distance3,
				borderColor0, borderColor1, borderColor2, borderColor3,
				borderDistance0, borderDistance1, borderDistance2, borderDistance3,
			)

			nextBaseX += heightA * font.getGlyphAdvance(glyph)
		}
	}

	fun drawFancyString(
		text: String, baseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, fillColor: Int, strokeColor: Int, strokeWidth: Float, alignment: TextAlignment,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
	) {
		drawFancyBorderedString(
			text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth, alignment,
			color0, color1, color2, color3, distance0, distance1, distance2, distance3,
			0, 0, 0, 0,
			12345f, 12345f, 12345f, 12345f,
		)
	}

	fun drawFancyShadowedString(
		text: String, baseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, fillColor: Int, strokeColor: Int, strokeWidth: Float,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
		shadowColor: Int, shadowOffsetX: Float, shadowOffsetY: Float, alignment: TextAlignment,
	) {
		drawString(text, baseX + shadowOffsetX, baseY + shadowOffsetY, heightA, font, shadowColor)
		drawFancyString(
			text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth, alignment,
			color0, color1, color2, color3, distance0, distance1, distance2, distance3
		)
	}
}
