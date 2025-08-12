package mardek.renderer

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.area.renderCurrentArea
import mardek.renderer.battle.renderBattle
import mardek.renderer.menu.renderInGameMenu
import mardek.state.ingame.InGameState
import mardek.state.util.Rectangle

internal fun renderInGame(context: RenderContext, state: InGameState, region: Rectangle): Vk2dColorBatch {

	var titleColorBatch: Vk2dColorBatch? = null
	val area = state.campaign.currentArea
	if (area != null) {
		val battle = area.activeBattle
		if (battle == null) {
			titleColorBatch = renderCurrentArea(context, area, region)
			if (state.menu.shown) titleColorBatch = renderInGameMenu(context, region, state.menu, state.campaign)
		} else {
			renderBattle(context, state.campaign, battle, region)
			val loot = area.battleLoot
			if (loot != null) TODO("Render battle loot")
		}
	}

	if (titleColorBatch == null) titleColorBatch = context.addColorBatch(12)
	return titleColorBatch
}
