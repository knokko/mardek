package mardek.renderer

import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
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
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import java.util.concurrent.CompletableFuture

class GameRenderer(
	private val resources: CompletableFuture<SharedResources>,
) {

	fun render(
		getContent: CompletableFuture<Content>, state: GameState, recorder: CommandRecorder,
		targetImage: VkbImage, framebuffer: Long, frameIndex: Int, soundQueue: SoundQueue,
	) {
		val resources = resources.join()
		val context = if (getContent.isDone) {
			// TODO Use a smaller width & height to leave space for the borders
			RenderContext(getContent.get(), resources, state, recorder, targetImage.width, targetImage.height, frameIndex, soundQueue)
		} else null
		resources.perFrameBuffer.startFrame(frameIndex)

		val renderer = createRenderer(state)
		if (context != null) renderer.beforeRendering(context)

		val clearValues = VkClearValue.calloc(1, recorder.stack)
		clearValues.get(0).color().float32(recorder.stack.floats(0f, 0f, 0f, 1f)) // TODO Let renderer decide background color

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
