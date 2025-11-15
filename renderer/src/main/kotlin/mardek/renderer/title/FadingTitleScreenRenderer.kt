package mardek.renderer.title

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderContext
import mardek.state.title.StartNewGameState
import mardek.state.util.Rectangle

internal fun renderFadingTitleScreen(
	context: RawRenderContext, fullRenderContext: RenderContext?,
	state: StartNewGameState, region: Rectangle,
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val batches = renderTitleScreen(context, fullRenderContext, state.titleState, region)

	val opacity = (System.nanoTime() - state.beginButtonClickTime).toFloat() / StartNewGameState.FADE_DURATION.toFloat()
	context.pipelines.base.color.addBatch(context.stage, 2).fill(
		region.minX, region.minY, region.maxX, region.maxY,
		rgba(0f, 0f, 0f, opacity),
	)

	return batches
}
