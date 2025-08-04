package mardek.renderer

import com.github.knokko.vk2d.Vk2dFrame
import mardek.renderer.title.renderTitleScreen
import mardek.state.ingame.InGameState
import mardek.state.title.GameOverState
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle

const val BORDER_WIDTH = 2
private const val BORDER_HEIGHT = 24
const val FULL_BORDER_HEIGHT = BORDER_WIDTH + BORDER_HEIGHT

private fun renderRegion(frame: Vk2dFrame) = Rectangle(
	BORDER_WIDTH, FULL_BORDER_HEIGHT,
	frame.width - 2 * BORDER_WIDTH,
	frame.height - BORDER_WIDTH - FULL_BORDER_HEIGHT
)

fun renderGame(context: RawRenderContext) {
	val state = context.state.currentState

	val titleBarBatch = when (state) {
		is TitleScreenState -> renderTitleScreen(context, state, renderRegion(context.frame))
		is GameOverState -> TODO()
		else -> context.pipelines.color.addBatch(context.frame, 36)
	}

	renderTitleBar(context.state, titleBarBatch)
}

fun renderGame(context: RenderContext) {
	val state = context.state.currentState

	val titleBarBatch = when (state) {
		is InGameState -> renderInGame(context, state, renderRegion(context.frame))
		else -> context.pipelines.color.addBatch(context.frame, 36)
	}

	renderTitleBar(context.state, titleBarBatch)
}
