package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.ui.TitleScreenRenderer
import mardek.state.GameState
import mardek.state.StartupState
import mardek.state.ingame.InGameState
import mardek.state.title.TitleScreenState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import java.util.concurrent.CompletableFuture

class GameRenderer(
	private val boiler: BoilerInstance,
	private val resources: CompletableFuture<SharedResources>,
) {

	fun render(
		state: GameState, recorder: CommandRecorder,
		targetImage: VkbImage, framebuffer: Long, frameIndex: Int
	) {
		if (state is StartupState) return

		resources.join().perFrameBuffer.startFrame(frameIndex)

		val renderer = createRenderer(state)
		renderer.beforeRendering(recorder, targetImage, frameIndex)

		val clearValues = VkClearValue.calloc(1, recorder.stack)
		clearValues.get(0).color().float32(recorder.stack.floats(0f, 0f, 0f, 1f)) // TODO Let renderer decide background color

		val biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack)
		biRenderPass.`sType$Default`()
		biRenderPass.renderPass(resources.join().renderPass)
		biRenderPass.framebuffer(framebuffer)
		biRenderPass.renderArea().offset().set(0, 0)
		biRenderPass.renderArea().extent().set(targetImage.width, targetImage.height)
		biRenderPass.pClearValues(clearValues)
		biRenderPass.clearValueCount(1)

		vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

		renderer.render(recorder, targetImage, frameIndex)

		vkCmdEndRenderPass(recorder.commandBuffer)
	}

	fun destroy() {
		resources.join().destroy()
	}

	private fun createRenderer(state: GameState): StateRenderer {
		if (state is InGameState) return InGameRenderer(state, boiler, resources.join())
		if (state is TitleScreenState) return TitleScreenRenderer(boiler, resources.join(), state)

		throw UnsupportedOperationException("Unexpected state $state")
	}

	companion object {
		fun addBoilerRequirements(builder: BoilerBuilder): BoilerBuilder = builder
			.requiredFeatures10 { it.textureCompressionBC() }
			.featurePicker10 { _, _, toEnable -> toEnable.textureCompressionBC(true) }
	}
}
