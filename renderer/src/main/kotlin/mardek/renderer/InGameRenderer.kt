package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.assets.Campaign
import mardek.renderer.area.AreaRenderer
import mardek.renderer.area.SharedAreaResources
import mardek.renderer.ui.InGameMenuRenderer
import mardek.renderer.ui.SharedUiResources
import mardek.state.ingame.InGameState

class InGameRenderer(
		private val assets: Campaign,
		private val state: InGameState,
		boiler: BoilerInstance,
		private val resources: SharedAreaResources,
		private val sharedUi: SharedUiResources,
): StateRenderer(boiler) {

	override fun beforeRendering(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val area = state.campaign.currentArea
		if (area != null) AreaRenderer(assets, area, state.campaign.characterSelection, resources).beforeRendering(recorder, targetImage, frameIndex)
	}

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val area = state.campaign.currentArea
		if (area != null) AreaRenderer(assets, area, state.campaign.characterSelection, resources).render(recorder, targetImage, frameIndex)
		if (state.menu.shown) InGameMenuRenderer(assets, sharedUi, state).render(recorder, targetImage, frameIndex)
	}
}
