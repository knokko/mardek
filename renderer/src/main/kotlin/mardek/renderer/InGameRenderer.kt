package mardek.renderer

import mardek.renderer.area.AreaRenderer
import mardek.renderer.battle.BattleRenderer
import mardek.renderer.battle.loot.BattleLootRenderer
import mardek.renderer.ui.InGameMenuRenderer
import mardek.state.ingame.InGameState

class InGameRenderer(
		private val state: InGameState,
): StateRenderer() {

	private var areaRenderer: AreaRenderer? = null
	private var battleRenderer: BattleRenderer? = null
	private var lootRenderer: BattleLootRenderer? = null
	private var menuRenderer: InGameMenuRenderer? = null

	override fun beforeRendering(context: RenderContext) {
		context.resources.kim1Renderer.begin()
		context.resources.kim2Renderer.begin()

		val inGameContext = InGameRenderContext(state.campaign, context)
		val area = state.campaign.currentArea
		areaRenderer = if (area != null && area.activeBattle == null) AreaRenderer(inGameContext, area) else null
		battleRenderer = if (area?.activeBattle != null) BattleRenderer(inGameContext, area.activeBattle!!) else null
		lootRenderer = if (area?.battleLoot != null) BattleLootRenderer(inGameContext) else null
		menuRenderer = if (state.menu.shown) InGameMenuRenderer(inGameContext, state.menu) else null

		areaRenderer?.beforeRendering()
		menuRenderer?.beforeRendering()
		battleRenderer?.beforeRendering()
		lootRenderer?.beforeRendering()

		context.resources.kim1Renderer.recordBeforeRenderpass(context.recorder, context.frameIndex)
	}

	override fun render(context: RenderContext) {
		context.uiRenderer.begin(context.recorder, context.targetImage)
		areaRenderer?.render()
		battleRenderer?.render()
		lootRenderer?.render()
		menuRenderer?.render()

		context.uiRenderer.end()
		context.resources.kim1Renderer.end()
		context.resources.kim2Renderer.end()
	}
}
