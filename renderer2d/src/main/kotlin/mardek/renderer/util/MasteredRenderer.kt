package mardek.renderer.util

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch

internal fun renderFancyMasteredText(
	context: RenderContext, batch: MardekGlyphBatch,
	baseX: Float, baseY: Float, heightA: Float,
) {
	val outerColor = srgbToLinear(rgb(213, 0, 0))
	val quarterColor = srgbToLinear(rgb(255, 81, 26))
	val middleColor = srgbToLinear(rgb(255, 147, 46))
	val font = context.bundle.getFont(context.content.fonts.fat.index)
	val strokeWidth = 0.3f * heightA / 8f
	batch.drawFancyString(
		"MASTERED", baseX, baseY, heightA, font, outerColor,
		srgbToLinear(rgb(255, 255, 153)),
		strokeWidth, TextAlignment.LEFT,
		quarterColor, middleColor, quarterColor, outerColor,
		0.2f, 0.4f, 0.7f, 0.9f
	)
}
