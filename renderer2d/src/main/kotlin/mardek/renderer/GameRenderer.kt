package mardek.renderer

import com.github.knokko.vk2d.Vk2dFrame
import mardek.content.Content
import mardek.renderer.title.renderTitleScreen
import mardek.state.GameStateManager
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle

const val BORDER_WIDTH = 2
private const val BORDER_HEIGHT = 24
const val FULL_BORDER_HEIGHT = BORDER_WIDTH + BORDER_HEIGHT

fun renderGame(context: RawRenderContext) {
	val renderRegion = Rectangle(
		BORDER_WIDTH, FULL_BORDER_HEIGHT,
		context.frame.width - 2 * BORDER_WIDTH,
		context.frame.height - BORDER_WIDTH - FULL_BORDER_HEIGHT
	)
	val state = context.state.currentState

	val titleBarBatch = if (state is TitleScreenState) renderTitleScreen(context, state, renderRegion)
	else context.pipelines.color.addBatch(context.frame, 36)

	renderTitleBar(context, titleBarBatch)
}

class GameRenderer(
	private val content: Content,
	private val state: GameStateManager,
	private val frame: Vk2dFrame,
) {

	fun render() {

	}
//	fun render(
//		getContent: CompletableFuture<Content>, state: GameStateManager, recorder: CommandRecorder,
//		targetImage: VkbImage, framebuffer: Long, frameIndex: Int, soundQueue: SoundQueue,
//	) {
//
//		val pScissor = VkRect2D.calloc(1, recorder.stack)
//		pScissor.offset().set(BORDER_WIDTH, FULL_BORDER_HEIGHT)
//		pScissor.extent().set(targetImage.width - 2 * BORDER_WIDTH, targetImage.height - FULL_BORDER_HEIGHT)
//
//		val context = if (getContent.isDone) {
//			RenderContext(
//				getContent.get(), resources, state.currentState, recorder,
//				pScissor.extent().width(), pScissor.extent().height(),
//				frameIndex, soundQueue
//			)
//		} else null
//		resources.perFrameBuffer.startFrame(frameIndex)
//
//		val renderer = createRenderer(state.currentState)
//		if (context != null) renderer.beforeRendering(context)
//
//		val clearValues = VkClearValue.calloc(1, recorder.stack)
//		clearValues.get(0).color().float32(recorder.stack.floats(0f, 0f, 0f, 0f))
//
//		val biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack)
//		biRenderPass.`sType$Default`()
//		biRenderPass.renderPass(resources.renderPass)
//		biRenderPass.framebuffer(framebuffer)
//		biRenderPass.renderArea().offset().set(0, 0)
//		biRenderPass.renderArea().extent().set(targetImage.width, targetImage.height)
//		biRenderPass.pClearValues(clearValues)
//		biRenderPass.clearValueCount(1)
//
//		vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
//
//		renderTitleBar(recorder, targetImage, resources.rectangleRenderer, pScissor, state)
//		if (context != null) renderer.render(context)
//
//		vkCmdEndRenderPass(recorder.commandBuffer)
//	}
//
//	private fun createRenderer(state: GameState): StateRenderer {
//		if (state is InGameState) return InGameRenderer(state)
//		if (state is TitleScreenState) return TitleScreenRenderer(state)
//		if (state is GameOverState) return GameOverRenderer(state)
//
//		throw UnsupportedOperationException("Unexpected state $state")
//	}
}
