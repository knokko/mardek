package mardek.renderer.util

import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.renderer.RenderContext

internal fun renderFancyMasteredText(
	context: RenderContext, batch: Vk2dFancyTextBatch,
	baseX: Float, baseY: Float, heightA: Float, alpha: Int = 255,
) {
	val font = context.bundle.getFont(context.content.fonts.fat.index)
	for (style in arrayOf(
		MardekTextStyles.masteredBack1(alpha),
		MardekTextStyles.masteredBack2(alpha),
		MardekTextStyles.masteredFront(alpha),
	)) {
		batch.drawString(
			"MASTERED", baseX, baseY, 0f, heightA, font,
			style, TextAlignment.LEFT,
		)
	}
}
