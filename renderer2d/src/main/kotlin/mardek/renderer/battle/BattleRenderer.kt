package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.RenderContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.util.Rectangle

internal fun renderBattle(
	context: RenderContext, state: CampaignState,
	battleState: BattleState, region: Rectangle
): Vk2dColorBatch {
	val battleContext = BattleRenderContext(context, state, battleState)

	val animationPartBatch = context.addAnimationPartBatch(1000) // TODO Choose nice capacity
	renderBattleBackground(battleContext, animationPartBatch)
	for (opponent in battleState.allOpponents().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, animationPartBatch, opponent).render()
	}
	for (player in battleState.allPlayers().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, animationPartBatch, player).render()
	}

	// Pretty much all components require colorBatch to be the first batch
	val colorBatch = context.addColorBatch(1000) // TODO Choose nice capacity

	// The action bar expects the oval batch to be behind the kim batch
	val ovalBatch = context.addOvalBatch(100) // TODO Choose nice capacity

	// The player block renderer expects the image batch to be behind the kim batch
	val imageBatch = context.addImageBatch(1000) // TODO Choose nice capacity
	val kimBatch = context.addKim3Batch(1000) // TODO Choose nice capacity
	val textBatch = context.addFancyTextBatch(1000) // TODO Choose nice capacity

	// The combatant info popup needs to render above everything else
	val lateColorBatch = context.addColorBatch(100) // TODO Choose nice capacity
	val lateKimBatch = context.addKim3Batch(100) // TODO Choose nice capacity
	val lateImageBatch = context.addImageBatch(100) // TODO Choose nice capacity
	val lateTextBatch = context.addTextBatch(100) // TODO Choose nice capacity

	renderTurnOrder(battleContext, colorBatch, kimBatch, textBatch, Rectangle(
		region.minX, region.minY + region.height / 12, region.width, region.height / 12
	))
	renderActionBar(battleContext, colorBatch, ovalBatch, kimBatch, imageBatch, textBatch, Rectangle(
		region.minX, region.boundY - region.height / 12 - region.height / 8,
		region.width, region.height / 12
	))

	renderSkillOrItemSelection(battleContext, colorBatch, ovalBatch, kimBatch, imageBatch, textBatch, Rectangle(
		region.minX + region.width / 3, region.minY + region.height / 5,
		width = region.width / 3, height = 4 * region.height / 7,
	))

	renderSkillOrItemDescription(battleContext, colorBatch, kimBatch, imageBatch, textBatch, Rectangle(
		region.minX, region.minY + region.height / 12, region.width, region.height / 9
	))

	renderCurrentMoveBar(battleContext, colorBatch, kimBatch, imageBatch, textBatch, Rectangle(
		region.minX, region.minY + region.height / 12, region.width, region.height / 16
	))

	for ((index, enemy) in battleContext.battle.opponents.withIndex()) {
		if (enemy == null) continue
		val region = Rectangle(
			minX = region.minX + index * region.width / 4, minY = region.minY,
			width = region.width / 4, height = region.height / 12
		)
//		indicatorRenderers.add(DamageIndicatorRenderer(context, enemy))
//		effectRenderers.add(EffectHistoryRenderer(context, enemy))
		if (enemy is MonsterCombatantState) {
			renderMonsterBlock(
				battleContext, enemy, colorBatch, lateColorBatch, ovalBatch,
				imageBatch, textBatch, region,
			)
		} else {
			renderPlayerBlock(
				battleContext, enemy as PlayerCombatantState, colorBatch, lateColorBatch, ovalBatch,
				kimBatch, imageBatch, textBatch, region,
			)
		}
	}

	for ((index, player) in battleContext.battle.players.reversed().withIndex()) {
		if (player == null) continue
		val region = Rectangle(
			minX = region.minX + index * region.width / 4,
			minY = region.boundY - region.height / 8,
			width = region.width / 4, height = region.height / 8,
		)
//		indicatorRenderers.add(DamageIndicatorRenderer(context, player))
//		effectRenderers.add(EffectHistoryRenderer(context, player))
		if (player is MonsterCombatantState) {
			renderMonsterBlock(
				battleContext, player, colorBatch, lateColorBatch, ovalBatch,
				imageBatch, textBatch, region,
			)
		} else {
			renderPlayerBlock(
				battleContext, player as PlayerCombatantState, colorBatch, lateColorBatch, ovalBatch,
				kimBatch, imageBatch, textBatch, region,
			)
		}
	}

	renderChallengeBar(battleContext, colorBatch, imageBatch, Rectangle(
		minX = region.minX,
		minY = region.boundY - region.height / 16 - region.height / 8,
		width = region.width,
		height = region.height / 16,
	))

	renderCombatantInfoPopup(battleContext, lateColorBatch, lateKimBatch, lateImageBatch, lateTextBatch, Rectangle(
		region.minX, region.minY + region.height / 8,
		region.width, region.boundY - region.height / 8 - region.height / 16,
	))

	return colorBatch
}
