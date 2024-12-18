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
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VkRenderingAttachmentInfo

class GameRenderer(
	private val boiler: BoilerInstance,
	targetImageFormat: Int,
	numFramesInFlight: Int,
	assetsPath: String,
) {

	private val resources = SharedResources(boiler, assetsPath, numFramesInFlight, targetImageFormat)

	fun render(
		state: GameState, recorder: CommandRecorder,
		targetImage: VkbImage, frameIndex: Int
	) {
		if (state is StartupState) return
		val renderer = createRenderer(state)
		renderer.beforeRendering(recorder, targetImage, frameIndex)

		val colorAttachments = VkRenderingAttachmentInfo.calloc(1, recorder.stack)
		recorder.simpleColorRenderingAttachment(
			colorAttachments.get(0), targetImage.vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
			0f, 0f, 0f, 1f // TODO Let renderer decide background color
		)
		recorder.beginSimpleDynamicRendering(
			targetImage.width, targetImage.height, colorAttachments, null, null
		)

		renderer.render(recorder, targetImage, frameIndex)

		recorder.endDynamicRendering()
	}

	fun destroy() {
		resources.destroy()
	}

	private fun createRenderer(state: GameState): StateRenderer {
		if (state is InGameState) return InGameRenderer(state, boiler, resources)
		if (state is TitleScreenState) return TitleScreenRenderer(boiler, resources, state)

		throw UnsupportedOperationException("Unexpected state $state")
	}

	companion object {
		fun addBoilerRequirements(builder: BoilerBuilder): BoilerBuilder = builder
			.enableDynamicRendering()
			.requiredFeatures10 { it.textureCompressionBC() }
			.featurePicker10 { _, _, toEnable -> toEnable.textureCompressionBC(true) }
	}
}
