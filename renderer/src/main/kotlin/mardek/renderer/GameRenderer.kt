package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.state.GameState
import mardek.state.InGameState
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VkRenderingAttachmentInfo

class GameRenderer(private val boiler: BoilerInstance) {

	private var lastState: GameState? = null
	private lateinit var currentRenderer: StateRenderer

	fun render(state: GameState, recorder: CommandRecorder, targetImage: VkbImage, targetImageFormat: Int) {
		if (state != lastState) {
			if (this::currentRenderer.isInitialized) currentRenderer.destroy()
			currentRenderer = createRenderer(state, targetImageFormat)
			lastState = state
		}

		val colorAttachments = VkRenderingAttachmentInfo.calloc(1, recorder.stack)
		recorder.simpleColorRenderingAttachment(
			colorAttachments.get(0), targetImage.vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
			0.3f, 0.6f, 0.9f, 1f // TODO Let renderer choose clear color
		)
		recorder.beginSimpleDynamicRendering(
			targetImage.width, targetImage.height, colorAttachments, null, null
		)

		currentRenderer.render(recorder, targetImage)

		recorder.endDynamicRendering()
	}

	fun destroy() {
		if (this::currentRenderer.isInitialized) currentRenderer.destroy()
	}

	private fun createRenderer(state: GameState, targetImageFormat: Int): StateRenderer = stackPush().use { stack ->
		if (state is InGameState) return InGameRenderer(state, boiler, targetImageFormat)

		throw UnsupportedOperationException("Unexpected state $state")
	}

	companion object {
		fun addBoilerRequirements(builder: BoilerBuilder): BoilerBuilder = builder
			.enableDynamicRendering()
			.requiredFeatures12 { it.shaderSampledImageArrayNonUniformIndexing() }
			.featurePicker12 { _, _, toEnable -> toEnable.shaderSampledImageArrayNonUniformIndexing(true) }
	}
}
