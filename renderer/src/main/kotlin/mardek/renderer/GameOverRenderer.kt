package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.title.titleScreenInfo
import mardek.state.title.GameOverState
import mardek.state.util.Rectangle
import kotlin.math.max

internal fun renderGameOver(
	context: RawRenderContext, state: GameOverState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val ovalBatch = context.pipelines.base.oval.addBatch(
		context.stage, context.perFrameDescriptorSet, 2
	)
	val circleColor = srgbToLinear(rgb(80, 0, 0))
	ovalBatch.complex(
		region.minX + region.width / 5 - 1, region.minY + region.height / 3 - 1,
		region.boundX - region.width / 5 + 1, region.boundY - region.height / 3 + 1,
		region.minX + 0.5f * region.width, region.minY + 0.5f * region.height,
		region.width * 0.3f, region.height / 6f,
		circleColor, changeAlpha(circleColor, 150), 0, 0, 0,
		0.1f, 1f, 1f, 1f,
	)

	val fancyTextBatch = context.pipelines.base.fancyText.addBatch(
		context.stage, 30, context.fancyTextStyleCache
	)
	val simpleTextBatch = context.pipelines.base.simpleText.addBatch(
		context.stage, 120, context.textStyleCache
	)
	val titleHeight = region.height / 12f
	val largeFont = context.titleScreenBundle.getFont(titleScreenInfo.largeFont.index)
	for (style in arrayOf(
		MardekTextStyles.TitleScreen.GAME_OVER_BACK1,
		MardekTextStyles.TitleScreen.GAME_OVER_BACK2,
		MardekTextStyles.TitleScreen.GAME_OVER_FRONT,
	)) {
		fancyTextBatch.drawString(
			"GAME   OVER", region.minX + 0.5f * region.width,
			region.minY + 0.5f * (region.height + titleHeight), 0f, titleHeight, largeFont,
			style, TextAlignment.CENTERED,
		)
	}

	val smallFont = context.titleScreenBundle.getFont(titleScreenInfo.basicFont.index)
	simpleTextBatch.drawString(
		"Press E or Q to return to the Title Screen", region.minX + 0.5f * region.width,
		region.minY + 0.65f * region.height, 0.025f * region.height, smallFont,
		srgbToLinear(rgb(255, 82, 82)), TextAlignment.CENTERED,
	)

	val colorBatch = context.pipelines.base.color.addBatch(context.stage, 40)
	val timeSinceGameOver = System.nanoTime() - state.startTime
	val fade = max(0L, 255L - 255L * timeSinceGameOver / 5000_000_000L).toInt()
	if (fade > 0) {
		colorBatch.fill(
			region.minX, region.minY, region.width, region.height,
			rgba(0, 0, 0, fade)
		)
	}
	return Pair(colorBatch, simpleTextBatch)
}
