package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.content.Content
import mardek.state.GameStateManager
import mardek.state.VideoSettings
import mardek.state.ingame.CampaignState
import mardek.state.util.Rectangle

class RawRenderContext(
	val stage: Vk2dRenderStage,
	val pipelines: MardekPipelines,
	val textBuffer: Vk2dTextBuffer,
	val perFrameDescriptorSet: Long,
	val recorder: CommandRecorder,
	val content: Content?,
	val state: GameStateManager,
	val titleScreenBundle: Vk2dResourceBundle,
	val videoSettings: VideoSettings,
	val currentFps: Long,
)

class RenderContext(
	val frame: Vk2dSwapchainFrame,
	var currentStage: Vk2dRenderStage,
	val framebuffers: MardekFramebuffers,
	val perFrame: PerFrameResources,
	val pipelines: MardekPipelines,
	val textBuffer: Vk2dTextBuffer,
	val perFrameDescriptorSet: Long,
	val recorder: CommandRecorder,
	val content: Content,
	val state: GameStateManager,
	val campaign: CampaignState,
	val bundle: Vk2dResourceBundle,
	val videoSettings: VideoSettings,
	val currentFps: Long,
) {
	fun addColorBatch(initialCapacity: Int) = pipelines.base.color.addBatch(currentStage, initialCapacity)!!

	fun addOvalBatch(initialCapacity: Int) = pipelines.base.oval.addBatch(
		currentStage, perFrameDescriptorSet, initialCapacity
	)!!

	fun addImageBatch(initialCapacity: Int) = pipelines.base.image.addBatch(currentStage, initialCapacity, bundle)!!

	fun addTextBatch(initialCapacity: Int) = pipelines.base.text.addBatch(
		currentStage, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)!!

	fun addFancyTextBatch(initialCapacity: Int) = pipelines.fancyText.addBatch(
		currentStage, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)

	fun addKim3Batch(initialCapacity: Int) = pipelines.base.kim3.addBatch(currentStage, initialCapacity, bundle, perFrameDescriptorSet)!!

	fun addAreaSpriteBatch(initialCapacity: Int, scissor: Rectangle) = pipelines.areaSprite.addBatch(
		currentStage, initialCapacity, bundle, perFrameDescriptorSet, scissor
	)

	fun addAreaLightBatch(scissor: Rectangle) = pipelines.areaLight.addBatch(currentStage, perFrameDescriptorSet, scissor)

	fun addAnimationPartBatch(initialCapacity: Int) = pipelines.animation.addBatch(currentStage, initialCapacity, bundle)
}
