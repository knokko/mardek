package mardek.renderer

import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.Content
import mardek.renderer.ui.GameOverRenderer
import mardek.renderer.ui.TitleScreenRenderer
import mardek.state.GameState
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.title.GameOverState
import mardek.state.title.TitleScreenState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkViewport
import java.util.concurrent.CompletableFuture

const val BORDER_WIDTH = 2
private const val BORDER_HEIGHT = 24
const val FULL_BORDER_HEIGHT = BORDER_WIDTH + BORDER_HEIGHT

class GameRenderer(
	private val resources: CompletableFuture<SharedResources>,
) {

	fun render(
		getContent: CompletableFuture<Content>, state: GameState, recorder: CommandRecorder,
		targetImage: VkbImage, framebuffer: Long, frameIndex: Int, soundQueue: SoundQueue,
	) {
		val borderColor = srgbToLinear(rgb(74, 58, 48))

		val pScissor = VkRect2D.calloc(1, recorder.stack)
		pScissor.offset().set(BORDER_WIDTH, FULL_BORDER_HEIGHT)
		pScissor.extent().set(targetImage.width - 2 * BORDER_WIDTH, targetImage.height - FULL_BORDER_HEIGHT)

		val resources = resources.join()
		val context = if (getContent.isDone) {
			RenderContext(
				getContent.get(), resources, state, recorder,
				pScissor.extent().width(), pScissor.extent().height(),
				frameIndex, soundQueue
			)
		} else null
		resources.perFrameBuffer.startFrame(frameIndex)

		val renderer = createRenderer(state)
		if (context != null) renderer.beforeRendering(context)

		val clearValues = VkClearValue.calloc(1, recorder.stack)
		clearValues.get(0).color().float32(recorder.stack.floats(0f, 0f, 0f, 1f))

		val biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack)
		biRenderPass.`sType$Default`()
		biRenderPass.renderPass(resources.renderPass)
		biRenderPass.framebuffer(framebuffer)
		biRenderPass.renderArea().offset().set(0, 0)
		biRenderPass.renderArea().extent().set(targetImage.width, targetImage.height)
		biRenderPass.pClearValues(clearValues)
		biRenderPass.clearValueCount(1)

		vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)

		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

		resources.rectangleRenderer.beginBatch(
			recorder, targetImage.width, targetImage.height, 1
		)
		resources.rectangleRenderer.fill(
			BORDER_WIDTH, BORDER_WIDTH,
			targetImage.width - 1 - BORDER_WIDTH,
			pScissor.offset().y() - 1, borderColor
		)
		resources.rectangleRenderer.endBatch(recorder)

		val pViewport = VkViewport.calloc(1, recorder.stack)
		pViewport.x(pScissor.offset().x().toFloat())
		pViewport.y(pScissor.offset().y().toFloat())
		pViewport.width(pScissor.extent().width().toFloat())
		pViewport.height(pScissor.extent().height().toFloat())
		pViewport.minDepth(0f)
		pViewport.maxDepth(1f)

		vkCmdSetViewport(recorder.commandBuffer, 0, pViewport)
		vkCmdSetScissor(recorder.commandBuffer, 0, pScissor)

		if (context != null) renderer.render(context)

		vkCmdEndRenderPass(recorder.commandBuffer)
	}

	fun destroy() {
		resources.join().destroy()
	}

	private fun createRenderer(state: GameState): StateRenderer {
		if (state is InGameState) return InGameRenderer(state)
		if (state is TitleScreenState) return TitleScreenRenderer(state)
		if (state is GameOverState) return GameOverRenderer(state)

		throw UnsupportedOperationException("Unexpected state $state")
	}

	companion object {
		fun addBoilerRequirements(builder: BoilerBuilder): BoilerBuilder = builder
			.requiredFeatures10("textureCompressionBC") { it.textureCompressionBC() }
			.featurePicker10 { _, _, toEnable -> toEnable.textureCompressionBC(true) }
	}
}
