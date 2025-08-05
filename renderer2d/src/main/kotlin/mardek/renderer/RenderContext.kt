package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.content.Content
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
	val areaSpritePipeline: AreaSpritePipeline,
	val perFrameDescriptorSet: Long,
	val content: Content,
	val state: GameStateManager,
	val campaign: CampaignState,
	val bundle: Vk2dResourceBundle
) {
	fun addColorBatch(initialCapacity: Int = 1000) = pipelines.color.addBatch(frame, initialCapacity)!!

	fun addKim3Batch(initialCapacity: Int = 1000) = pipelines.kim3.addBatch(frame, initialCapacity, bundle)!!

	fun addAreaSpriteBatch(scissor: Rectangle) = areaSpritePipeline.addBatch(frame, bundle, scissor)
}
