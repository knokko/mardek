package mardek.renderer.util

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch

internal fun renderFancyMasteredText(
	context: RenderContext, batch: MardekGlyphBatch,
	baseX: Float, baseY: Float, heightA: Float, alpha: Int = 255,
) {
	val outerColor = srgbToLinear(rgba(213, 0, 0, alpha))
	val quarterColor = srgbToLinear(rgba(255, 81, 26, alpha))
	val middleColor = srgbToLinear(rgba(255, 147, 46, alpha))
	val font = context.bundle.getFont(context.content.fonts.fat.index)
	val strokeWidth = 0.3f * heightA / 8f
	batch.drawFancyString(
		"MASTERED", baseX, baseY, heightA, font, outerColor,
		srgbToLinear(rgba(255, 255, 153, alpha)),
		strokeWidth, TextAlignment.LEFT,
		quarterColor, middleColor, quarterColor, outerColor,
		0.2f, 0.4f, 0.7f, 0.9f
	)
}
