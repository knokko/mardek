package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.area.AreaRenderer
import mardek.renderer.battle.BattleRenderer
import mardek.renderer.ui.InGameMenuRenderer
import mardek.state.ingame.InGameState

class InGameRenderer(
		private val state: InGameState,
		boiler: BoilerInstance,
		private val resources: SharedResources,
): StateRenderer(boiler) {

	private var areaRenderer: AreaRenderer? = null
	private var battleRenderer: BattleRenderer? = null
	private var menuRenderer: InGameMenuRenderer? = null

	override fun beforeRendering(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		resources.kim1Renderer.begin()
		resources.kim2Renderer.begin()

		val area = state.campaign.currentArea
		areaRenderer = if (area != null && area.activeBattle == null) AreaRenderer(
			recorder, targetImage, area, state, resources
		) else null
		battleRenderer = if (area?.activeBattle != null) BattleRenderer(
			recorder, targetImage, area.activeBattle!!, resources
		) else null
		menuRenderer = if (state.menu.shown) InGameMenuRenderer(
			recorder, targetImage, frameIndex, resources, state
		) else null

		areaRenderer?.beforeRendering()
		menuRenderer?.beforeRendering()

		resources.kim1Renderer.recordBeforeRenderpass(recorder, frameIndex)
	}

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val uiRenderer = resources.uiRenderers[frameIndex]
		uiRenderer.begin(recorder, targetImage)
		areaRenderer?.render(frameIndex)
		battleRenderer?.render(frameIndex)
		menuRenderer?.render(uiRenderer)

		uiRenderer.end()
		resources.kim1Renderer.end()
		resources.kim2Renderer.end()
	}
}
