package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.RenderContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleState
import mardek.state.util.Rectangle

internal fun renderBattle(
	context: RenderContext, state: CampaignState,
	battleState: BattleState, region: Rectangle
): Vk2dColorBatch {
	val battleContext = BattleRenderContext(context, state, battleState)

	val combatantBatch = context.addImageBatch(1000) // TODO Choose nice capacity
	renderBattleBackground(battleContext, combatantBatch)
	for (opponent in battleState.allOpponents().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, combatantBatch, opponent).render()
	}
	for (player in battleState.allPlayers().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, combatantBatch, player).render()
	}

	val colorBatch = context.addColorBatch(1000) // TODO Choose nice capacity
	return colorBatch
}
