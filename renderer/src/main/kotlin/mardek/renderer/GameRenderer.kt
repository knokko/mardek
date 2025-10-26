package mardek.renderer

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import mardek.renderer.title.renderTitleScreen
import mardek.renderer.title.titleScreenInfo
import mardek.state.ingame.InGameState
import mardek.state.title.GameOverState
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle

const val BORDER_WIDTH = 2
private const val BORDER_HEIGHT = 24
const val FULL_BORDER_HEIGHT = BORDER_WIDTH + BORDER_HEIGHT

private fun renderRegion(swapchainStage: Vk2dRenderStage) = Rectangle(
	BORDER_WIDTH, FULL_BORDER_HEIGHT,
	swapchainStage.width - 2 * BORDER_WIDTH,
	swapchainStage.height - BORDER_WIDTH - FULL_BORDER_HEIGHT
)

fun renderGame(context: RawRenderContext, fullContext: RenderContext?) {
	val state = context.state.currentState

	val (titleBarBatch, textBatch) = when (state) {
		is TitleScreenState -> renderTitleScreen(
			context, fullContext, state,
			renderRegion(context.stage),
		)
		is GameOverState -> renderGameOver(context, state, renderRegion(context.stage))
		else -> Pair(
			context.pipelines.base.color.addBatch(context.stage, 36),
			context.pipelines.base.text.addBatch(
				context.stage, 25, context.recorder,
				context.textBuffer, context.perFrameDescriptorSet,
			),
		)
	}

	renderTitleBar(
		context.state, titleBarBatch, textBatch,
		context.titleScreenBundle.getFont(titleScreenInfo.basicFont.index),
		if (context.videoSettings.showFps) context.currentFps else null,
	)
}

fun renderGame(context: RenderContext) {
	val state = context.state.currentState

	val (titleColorBatch, titleTextBatch) = when (state) {
		is InGameState -> renderInGame(context, state, renderRegion(context.frame.swapchainStage))
		else -> Pair(context.addColorBatch(36), context.addTextBatch(25))
	}

	renderTitleBar(
		context.state, titleColorBatch, titleTextBatch,
		context.bundle.getFont(context.content.fonts.basic1.index),
		if (context.videoSettings.showFps) context.currentFps else null,
	)
}
