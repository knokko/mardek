package mardek.renderer.actions

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.ActionShowChapterName
import mardek.renderer.MardekTextStyles
import mardek.renderer.RenderContext
import mardek.state.util.Rectangle
import kotlin.math.pow
import kotlin.time.Duration

internal fun renderChapterNameAndNumber(
	context: RenderContext, simpleTextBatch: Vk2dSimpleTextBatch, fancyTextBatch: Vk2dFancyTextBatch,
	action: ActionShowChapterName, timeSinceNodeStart: Duration, region: Rectangle,
) {
	var opacity = if (timeSinceNodeStart < ActionShowChapterName.FADE_DURATION) {
		timeSinceNodeStart / ActionShowChapterName.FADE_DURATION
	} else if (timeSinceNodeStart < ActionShowChapterName.FADE_DURATION + ActionShowChapterName.MAIN_DURATION) {
		1.0
	} else {
		(ActionShowChapterName.TOTAL_DURATION - timeSinceNodeStart) / ActionShowChapterName.FADE_DURATION
	}
	opacity = opacity.pow(3)

	val font = context.bundle.getFont(context.content.fonts.large2.index)
	val chapterNumberText = when (action.chapter) {
		1 -> "I"
		else -> throw IllegalArgumentException("Unexpected chapter number ${action.chapter}")
	}
	val numberColor = srgbToLinear(rgb(100, 66, 0))
	simpleTextBatch.drawString(
		chapterNumberText, region.minX + region.width * 0.5f,
		region.minY + region.height * 0.7f, 0.35f * region.height, font,
		changeAlpha(numberColor, opacity.toFloat()), TextAlignment.CENTERED,
	)

	fancyTextBatch.drawString(
		"Chapter ${action.chapter}: ${action.name}", region.minX + region.width * 0.5f,
		region.minY + 0.52f * region.height, 0f, 0.038f * region.height, font,
		MardekTextStyles.Cutscenes.chapterName(opacity.toFloat()), TextAlignment.CENTERED,
	)
}
