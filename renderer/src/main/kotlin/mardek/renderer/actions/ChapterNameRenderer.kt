package mardek.renderer.actions

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.ActionShowChapterName
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.util.Rectangle
import kotlin.math.pow

internal fun renderChapterNameAndNumber(
	context: RenderContext, textBatch: MardekGlyphBatch,
	action: ActionShowChapterName, relativeTime: Long, region: Rectangle,
) {
	var opacity = if (relativeTime < ActionShowChapterName.FADE_DURATION) {
		relativeTime.toFloat() / ActionShowChapterName.FADE_DURATION.toFloat()
	} else if (relativeTime < ActionShowChapterName.FADE_DURATION + ActionShowChapterName.MAIN_DURATION) {
		1f
	} else {
		(ActionShowChapterName.TOTAL_DURATION - relativeTime).toFloat() /
				ActionShowChapterName.FADE_DURATION.toFloat()
	}
	opacity = opacity.pow(3)

	val font = context.bundle.getFont(context.content.fonts.large2.index)
	val chapterNumberText = when (action.chapter) {
		1 -> "I"
		else -> throw IllegalArgumentException("Unexpected chapter number ${action.chapter}")
	}
	val numberColor = srgbToLinear(rgb(100, 66, 0))
	textBatch.drawString(
		chapterNumberText, region.minX + region.width * 0.5f,
		region.minY + region.height * 0.7f, 0.35f * region.height, font,
		changeAlpha(numberColor, opacity), TextAlignment.CENTERED,
	)

	var innerColor = srgbToLinear(rgb(241, 226, 188))
	innerColor = changeAlpha(innerColor, opacity)
	var outerColor = srgbToLinear(rgb(232, 198, 124))
	outerColor = changeAlpha(outerColor, opacity)
	textBatch.drawFancyString(
		"Chapter ${action.chapter}: ${action.name}", region.minX + region.width * 0.5f,
		region.minY + 0.52f * region.height, 0.038f * region.height, font,
		outerColor, 0, 0f, TextAlignment.CENTERED,
		outerColor, innerColor, innerColor, outerColor,
		0.27f, 0.27f, 0.65f, 0.65f,
	)
}
