package mardek.renderer

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import mardek.renderer.title.renderTitleScreen
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

fun renderGame(context: RawRenderContext) {
	val state = context.state.currentState

	val titleBarBatch = when (state) {
		is TitleScreenState -> renderTitleScreen(context, state, renderRegion(context.stage))
		is GameOverState -> renderGameOver(context, state, renderRegion(context.stage))
		else -> context.pipelines.color.addBatch(context.stage, 36)
	}

	renderTitleBar(context.state, titleBarBatch)
}

fun renderGame(context: RenderContext) {
	val state = context.state.currentState

	val titleBarBatch = when (state) {
		is InGameState -> renderInGame(context, state, renderRegion(context.frame.swapchainStage))
		else -> context.pipelines.color.addBatch(context.frame.swapchainStage, 36)
	}

	renderTitleBar(context.state, titleBarBatch)
}
