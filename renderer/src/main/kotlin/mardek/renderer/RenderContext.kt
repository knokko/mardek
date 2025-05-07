package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.content.Content
import mardek.state.GameState
import mardek.state.ingame.CampaignState

open class RenderContext(
	val resources: SharedResources,
	val state: GameState,
	val recorder: CommandRecorder,
	val targetImage: VkbImage,
	val frameIndex: Int
) {
	val uiRenderer = resources.uiRenderers[frameIndex]
}

open class InGameRenderContext(
	val content: Content,
	val campaign: CampaignState,
	context: RenderContext,
) : RenderContext(context.resources, context.state, context.recorder, context.targetImage, context.frameIndex)
