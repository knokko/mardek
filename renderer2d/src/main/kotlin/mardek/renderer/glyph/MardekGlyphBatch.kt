package mardek.renderer.glyph

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.Vk2dFont
import com.github.knokko.vk2d.text.Vk2dTextBuffer

class MardekGlyphBatch(
	pipeline: MardekGlyphPipeline,
	frame: Vk2dFrame,
	initialCapacity: Int,
	recorder: CommandRecorder,
	textBuffer: Vk2dTextBuffer,
	perFrameDescriptorSet: Long,
): Vk2dGlyphBatch(pipeline, frame, initialCapacity, recorder, textBuffer, perFrameDescriptorSet) {

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
		borderColor0: Int, borderColor1: Int, borderDistance0: Float, borderDistance1: Float,
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
			glyphInfo.putInt(borderColor0).putInt(borderColor1).putInt(0).putInt(0)

			// GlyphInfo.borderDistances
			glyphInfo.putFloat(borderDistance0).putFloat(borderDistance1).putFloat(borderDistance1).putFloat(borderDistance1)
		}
	}

	fun drawFancyBorderedString(
		text: String, baseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, fillColor: Int, strokeColor: Int, strokeWidth: Float,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
		borderColor0: Int, borderColor1: Int, borderDistance0: Float, borderDistance1: Float,
	) {
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
				borderColor0, borderColor1,
				borderDistance0, borderDistance1,
			)

			nextBaseX += heightA * font.getGlyphAdvance(glyph)
		}
	}

	fun drawFancyString(
		text: String, baseX: Float, baseY: Float, heightA: Float,
		font: Vk2dFont, fillColor: Int, strokeColor: Int, strokeWidth: Float,
		color0: Int, color1: Int, color2: Int, color3: Int,
		distance0: Float, distance1: Float, distance2: Float, distance3: Float,
	) {
		drawFancyBorderedString(
			text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth, color0, color1, color2, color3,
			distance0, distance1, distance2, distance3, 0, 0, 12345f, 12345f
		)
	}
}
