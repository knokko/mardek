package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.area.AreaRenderer
import mardek.renderer.ui.InGameMenuRenderer
import mardek.state.ingame.InGameState

class InGameRenderer(
		private val state: InGameState,
		boiler: BoilerInstance,
		private val resources: SharedResources,
): StateRenderer(boiler) {

	private var areaRenderer: AreaRenderer? = null
	private var menuRenderer: InGameMenuRenderer? = null

	override fun beforeRendering(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		resources.kim1Renderer.begin()
		resources.kim2Renderer.begin()

		val area = state.campaign.currentArea
		areaRenderer = if (area != null) AreaRenderer(
			recorder, targetImage, area, state.campaign.characterSelection, resources
		) else null
		menuRenderer = if (state.menu.shown) InGameMenuRenderer(
			recorder, targetImage, frameIndex, resources, state
		) else null

		areaRenderer?.beforeRendering()
		menuRenderer?.beforeRendering()

		resources.kim1Renderer.recordBeforeRenderpass(recorder, frameIndex)
	}

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		areaRenderer?.render()
		menuRenderer?.render()

		resources.kim1Renderer.end()
		resources.kim2Renderer.end()
	}
}
