package mardek.renderer

import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.content.Content
import mardek.state.GameState
import mardek.state.GameStateManager
import mardek.state.SoundQueue

class RawRenderContext(
	val frame: Vk2dFrame,
	val resources: RenderResources,
	val content: Content?,
	val state: GameStateManager,
	val titleScreenBundle: Vk2dResourceBundle,
)

class RenderContext(
	val frame: Vk2dFrame,
	val resources: RenderResources,
	val content: Content,
	val state: GameState,
	val soundQueue: SoundQueue,
)
