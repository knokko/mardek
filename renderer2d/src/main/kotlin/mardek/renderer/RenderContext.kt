package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.content.Content
import mardek.renderer.area.AreaLightPipeline
import mardek.renderer.area.AreaSpritePipeline
import mardek.renderer.glyph.MardekGlyphPipeline
import mardek.state.GameStateManager
import mardek.state.ingame.CampaignState
import mardek.state.util.Rectangle

class RawRenderContext(
	val stage: Vk2dRenderStage,
	val pipelines: Vk2dPipelines,
	val textPipeline: MardekGlyphPipeline,
	val textBuffer: Vk2dTextBuffer,
	val perFrameDescriptorSet: Long,
	val recorder: CommandRecorder,
	val content: Content?,
	val state: GameStateManager,
	val titleScreenBundle: Vk2dResourceBundle,
)

class RenderContext(
	val frame: Vk2dSwapchainFrame,
	var currentStage: Vk2dRenderStage,
	val framebuffers: MardekFramebuffers,
	val perFrame: PerFrameResources,
	val pipelines: Vk2dPipelines,
	val fancyTextPipeline: MardekGlyphPipeline,
	val areaSpritePipeline: AreaSpritePipeline,
	val areaLightPipeline: AreaLightPipeline,
	val textBuffer: Vk2dTextBuffer,
	val perFrameDescriptorSet: Long,
	val recorder: CommandRecorder,
	val content: Content,
	val state: GameStateManager,
	val campaign: CampaignState,
	val bundle: Vk2dResourceBundle,
) {
	fun addColorBatch(initialCapacity: Int) = pipelines.color.addBatch(currentStage, initialCapacity)!!

	fun addOvalBatch(initialCapacity: Int) = pipelines.oval.addBatch(
		currentStage, perFrameDescriptorSet, initialCapacity
	)!!

	fun addImageBatch(initialCapacity: Int) = pipelines.image.addBatch(currentStage, initialCapacity, bundle)!!

	fun addTextBatch(initialCapacity: Int) = pipelines.text.addBatch(
		currentStage, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)!!

	fun addFancyTextBatch(initialCapacity: Int) = fancyTextPipeline.addBatch(
		currentStage, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)

	fun addKim3Batch(initialCapacity: Int) = pipelines.kim3.addBatch(currentStage, initialCapacity, bundle)!!

	fun addAreaSpriteBatch(initialCapacity: Int, scissor: Rectangle) = areaSpritePipeline.addBatch(
		currentStage, initialCapacity, bundle, perFrameDescriptorSet, scissor
	)

	fun addAreaLightBatch(scissor: Rectangle) = areaLightPipeline.addBatch(currentStage, perFrameDescriptorSet, scissor)
}
