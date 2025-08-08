package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.Vk2dFrame
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
	val frame: Vk2dFrame,
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
	val frame: Vk2dFrame,
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
	val bundle: Vk2dResourceBundle
) {
	fun addColorBatch(initialCapacity: Int) = pipelines.color.addBatch(frame, initialCapacity)!!

	fun addImageBatch(initialCapacity: Int) = pipelines.image.addBatch(frame, initialCapacity)!!

	fun addTextBatch(initialCapacity: Int) = pipelines.text.addBatch(
		frame, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)!!

	fun addFancyTextBatch(initialCapacity: Int) = fancyTextPipeline.addBatch(
		frame, initialCapacity, recorder, textBuffer, perFrameDescriptorSet
	)

	fun addKim3Batch(initialCapacity: Int) = pipelines.kim3.addBatch(frame, initialCapacity, bundle)!!

	fun addAreaSpriteBatch(initialCapacity: Int, scissor: Rectangle) = areaSpritePipeline.addBatch(
		frame, initialCapacity, bundle, perFrameDescriptorSet, scissor
	)

	fun addAreaLightBatch(scissor: Rectangle) = areaLightPipeline.addBatch(frame, perFrameDescriptorSet, scissor)
}
