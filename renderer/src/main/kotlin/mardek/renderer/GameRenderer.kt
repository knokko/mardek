package mardek.renderer

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import mardek.renderer.title.renderFadingTitleScreen
import mardek.renderer.title.renderMusicPlayer
import mardek.renderer.title.renderTitleScreen
import mardek.state.ingame.InGameState
import mardek.state.title.GameOverState
import mardek.state.title.MusicPlayerState
import mardek.state.title.StartNewGameState
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle

const val BORDER_WIDTH = 2
private const val BORDER_HEIGHT = 24
const val FULL_BORDER_HEIGHT = BORDER_WIDTH + BORDER_HEIGHT

private fun renderRegion(swapchainStage: Vk2dRenderStage, fullscreen: Boolean) = if (fullscreen) Rectangle(
	0, 0, swapchainStage.width, swapchainStage.height
) else Rectangle(
	BORDER_WIDTH, FULL_BORDER_HEIGHT,
	swapchainStage.width - 2 * BORDER_WIDTH,
	swapchainStage.height - BORDER_WIDTH - FULL_BORDER_HEIGHT
)

fun renderGame(context: RawRenderContext, fullContext: RenderContext?) {
	val state = context.state.currentState
	val renderRegion = renderRegion(context.stage, context.videoSettings.fullscreen)

	val (titleBarBatch, textBatch) = when (state) {
		is TitleScreenState -> renderTitleScreen(context, fullContext, state, renderRegion)
		is StartNewGameState -> renderFadingTitleScreen(context, fullContext, state, renderRegion)
		is GameOverState -> renderGameOver(context, state, renderRegion)
		is MusicPlayerState -> renderMusicPlayer(context, state, renderRegion)
		else -> Pair(
			context.pipelines.color.addBatch(context.stage, 36),
			context.pipelines.simpleText.addBatch(context.stage, 25, context.textStyleCache),
		)
	}

	if (!context.videoSettings.fullscreen) {
		renderTitleBar(
			context.state, titleBarBatch, textBatch,
			context.titleScreenBundle.getFont(context.titleContent.basicFont.index),
			if (context.videoSettings.showFps) context.currentFps else null,
		)
	}
}

fun renderGame(context: RenderContext) {
	val state = context.state.currentState
	val renderRegion = renderRegion(context.frame.swapchainStage, context.userSettings.videoSettings.fullscreen)

	val (titleColorBatch, titleTextBatch) = when (state) {
		is InGameState -> renderInGame(context, state, renderRegion)
		else -> Pair(context.addColorBatch(36), context.addTextBatch(25))
	}

	if (!context.userSettings.videoSettings.fullscreen) {
		renderTitleBar(
			context.state, titleColorBatch, titleTextBatch,
			context.bundle.getFont(context.content.fonts.basic1.index),
			if (context.userSettings.videoSettings.showFps) context.currentFps else null,
		)
	}
}
