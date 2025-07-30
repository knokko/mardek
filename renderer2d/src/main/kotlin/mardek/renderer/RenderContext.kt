package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.content.Content
import mardek.renderer.glyph.MardekGlyphPipeline
import mardek.state.GameState
import mardek.state.GameStateManager
import mardek.state.SoundQueue

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
	val content: Content,
	val state: GameState,
	val soundQueue: SoundQueue,
)
