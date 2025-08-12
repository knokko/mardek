package mardek.renderer.battle

import mardek.renderer.RenderContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleState
import mardek.state.util.Rectangle

internal fun renderBattle(context: RenderContext, state: CampaignState, battleState: BattleState, region: Rectangle) {
	val battleContext = BattleRenderContext(context, state, battleState)
}
