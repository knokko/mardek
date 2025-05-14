package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.content.Content
import mardek.state.GameState
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.UiUpdateContext

open class RenderContext(
	val content: Content,
	val resources: SharedResources,
	val state: GameState,
	val recorder: CommandRecorder,
	val targetImage: VkbImage,
	val frameIndex: Int
) {
	val uiRenderer = resources.uiRenderers[frameIndex]
}

open class InGameRenderContext(
	val campaign: CampaignState,
	context: RenderContext,
) : RenderContext(context.content, context.resources, context.state, context.recorder, context.targetImage, context.frameIndex) {

	val uiContext = UiUpdateContext(
		campaign.characterSelection, campaign.characterStates, SoundQueue(), content.audio.fixedEffects, content.skills
	)
}
