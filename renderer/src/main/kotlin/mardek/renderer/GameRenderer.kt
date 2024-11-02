package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.ui.SharedUiResources
import mardek.renderer.ui.TitleScreenRenderer
import mardek.state.GameState
import mardek.state.InGameState
import mardek.state.title.TitleScreenState
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VkRenderingAttachmentInfo

class GameRenderer(
	private val boiler: BoilerInstance,
	private val targetImageFormat: Int,
	private val numFramesInFlight: Int
) {

	private var lastState: GameState? = null
	private lateinit var currentRenderer: StateRenderer

	private val ui = SharedUiResources(boiler, targetImageFormat, numFramesInFlight)

	fun render(
		state: GameState, recorder: CommandRecorder,
		targetImage: VkbImage, frameIndex: Int
	) {
		if (state != lastState) {
			if (this::currentRenderer.isInitialized) currentRenderer.destroy()
			currentRenderer = createRenderer(state, targetImageFormat, numFramesInFlight)
			lastState = state
		}

		val colorAttachments = VkRenderingAttachmentInfo.calloc(1, recorder.stack)
		recorder.simpleColorRenderingAttachment(
			colorAttachments.get(0), targetImage.vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
			0f, 0f, 0f, 1f
		)
		recorder.beginSimpleDynamicRendering(
			targetImage.width, targetImage.height, colorAttachments, null, null
		)

		currentRenderer.render(recorder, targetImage, frameIndex)

		recorder.endDynamicRendering()
	}

	fun destroy() {
		if (this::currentRenderer.isInitialized) currentRenderer.destroy()
		ui.destroy()
	}

	private fun createRenderer(state: GameState, targetImageFormat: Int, numFramesInFlight: Int): StateRenderer {
		if (state is InGameState) return InGameRenderer(state, boiler, targetImageFormat, numFramesInFlight)
		if (state is TitleScreenState) return TitleScreenRenderer(boiler, ui)

		throw UnsupportedOperationException("Unexpected state $state")
	}

	companion object {
		fun addBoilerRequirements(builder: BoilerBuilder): BoilerBuilder = builder
			.enableDynamicRendering()
			.requiredFeatures12 { it.shaderSampledImageArrayNonUniformIndexing() }
			.featurePicker12 { _, _, toEnable -> toEnable.shaderSampledImageArrayNonUniformIndexing(true) }
	}
}
