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
		backgroundColor: Int,
		strokeWidth: Float
	) {
		val oldGlyphInfo = vertices.last().vertexData[1]
		val oldPosition = oldGlyphInfo.position()
		super.glyphAt(baseX, baseY, font, heightA, glyph, fillColor, strokeColor, backgroundColor, strokeWidth)
		val glyphInfo = vertices.last().vertexData[1]
		if (oldGlyphInfo !== glyphInfo || oldPosition != glyphInfo.position()) {
			glyphInfo.putFloat(baseY).putFloat(heightA)
		}
	}
}
