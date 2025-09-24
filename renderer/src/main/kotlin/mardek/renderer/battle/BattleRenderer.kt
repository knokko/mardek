package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import mardek.renderer.RenderContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.util.Rectangle

internal fun renderBattle(
	context: RenderContext, state: CampaignState,
	battleState: BattleState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val battleContext = BattleRenderContext(context, state, battleState)

	val animationPartBatch = context.addAnimationPartBatch(1000) // TODO Choose nice capacity
	renderBattleBackground(battleContext, animationPartBatch, Rectangle(
		region.minX, region.minY + region.height / 12, region.width,
		region.height - region.height / 12 - region.height / 8,
	))

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
	val lateOvalBatch = context.addOvalBatch(4)
	val lateKimBatch = context.addKim3Batch(100) // TODO Choose nice capacity
	val lateImageBatch = context.addImageBatch(100) // TODO Choose nice capacity
	val lateTextBatch = context.addTextBatch(100) // TODO Choose nice capacity
	val lateAnimationPartBatch = context.addAnimationPartBatch(100) // TODO Choose nice capacity

	for (opponent in battleState.allOpponents().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, animationPartBatch, opponent).render()
		renderDamageIndicator(battleContext, imageBatch, textBatch, opponent)
		renderEffectHistory(battleContext, opponent, imageBatch, textBatch, lateColorBatch)
	}
	for (player in battleState.allPlayers().sortedBy { it.getPosition(battleContext.battle).y }) {
		CombatantRenderer(battleContext, animationPartBatch, player).render()
		renderDamageIndicator(battleContext, imageBatch, textBatch, player)
		renderEffectHistory(battleContext, player, imageBatch, textBatch, lateColorBatch)
	}

	renderBattlePortrait(battleContext, animationPartBatch, region)
	renderBaseParticles(battleContext, imageBatch) // TODO Use separate particle batch?
	renderEffectParticles(battleContext, imageBatch)

	renderTurnOrder(battleContext, colorBatch, kimBatch, textBatch, Rectangle(
		region.minX, region.minY + region.height / 12, region.width, region.height / 12
	))
	renderThrownItems(battleContext, kimBatch)
	renderTargetSelection(battleContext, colorBatch, ovalBatch, imageBatch, textBatch, Rectangle(
		minX = region.minX, minY = region.minY + region.height / 12, width = region.width,
		height = region.height - region.height / 8 - region.height / 12
	))

	val actionBarRegion = Rectangle(
		region.minX, region.boundY - region.height / 12 - region.height / 8,
		region.width, computeActionBarHeight(region.height),
	)

	val stateMachine = battleContext.battle.state
	if (stateMachine is BattleStateMachine.SelectMove) {
		val selectedMove = stateMachine.selectedMove
		val isChoosingSkillOrItem = when (selectedMove) {
			is BattleMoveSelectionSkill -> selectedMove.skill != null
			is BattleMoveSelectionItem -> selectedMove.item != null
			else -> false
		}
		val isSelectingTarget = when (selectedMove) {
			is BattleMoveSelectionAttack -> selectedMove.target != null
			is BattleMoveSelectionSkill -> selectedMove.target != null
			is BattleMoveSelectionItem -> selectedMove.target != null
			else -> false
		}

		if (isChoosingSkillOrItem && !isSelectingTarget) {
			renderActionBar(
				ActionBarRenderMode.Background, battleContext, colorBatch, ovalBatch,
				kimBatch, imageBatch, textBatch, actionBarRegion
			)

			val framebuffers = context.framebuffers
			val battleRenderStage = context.currentStage

			context.currentStage = context.pipelines.base.blur.addSourceStage(
				context.frame, framebuffers.actionBarBlur, -1
			)
			context.pipelines.base.blur.addComputeStage(
				context.frame, context.perFrame.actionBarBlurDescriptors,
				framebuffers.actionBarBlur, 9, 50, -1
			)
			// TODO Determine the right capacities
			val blurColorBatch = context.addColorBatch(100)
			val blurOvalBatch = context.addOvalBatch(100)
			val blurKimBatch = context.addKim3Batch(100)
			val blurImageBatch = context.addImageBatch(100)
			val blurTextBatch = context.addTextBatch(100)
			renderActionBar(
				ActionBarRenderMode.BlurredBackground, battleContext,
				blurColorBatch, blurOvalBatch, blurKimBatch, blurImageBatch, blurTextBatch,
				Rectangle(0, 0, actionBarRegion.width, actionBarRegion.height)
			)

			context.currentStage = battleRenderStage
			context.pipelines.base.blur.addBatch(
				battleRenderStage, framebuffers.actionBarBlur,
				context.perFrame.actionBarBlurDescriptors,
				actionBarRegion.minX.toFloat(), actionBarRegion.minY.toFloat(),
				actionBarRegion.boundX.toFloat(), actionBarRegion.boundY.toFloat(),
			).fixedColorTransform(0, rgba(1f, 1f, 1f, 0.5f))
			renderActionBar(
				ActionBarRenderMode.Foreground, battleContext, lateColorBatch, lateOvalBatch,
				lateKimBatch, lateImageBatch, lateTextBatch, actionBarRegion
			)
		} else if (!isSelectingTarget) {
			for (renderMode in ActionBarRenderMode.entries) {
				renderActionBar(
					renderMode, battleContext, colorBatch, ovalBatch,
					kimBatch, imageBatch, textBatch, actionBarRegion
				)
			}
		}
	}

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

	val lightBarColor = srgbToLinear(rgb(88, 74, 43))
	val darkBarColor = srgbToLinear(rgb(37, 28, 17))
	colorBatch.gradient(
		region.minX, region.minY, region.maxX, region.minY + region.height / 12,
		lightBarColor, darkBarColor, lightBarColor
	)
	colorBatch.gradient(
		region.minX, region.boundY - region.height / 8, region.maxX, region.maxY,
		darkBarColor, lightBarColor, darkBarColor,
	)
	for ((index, enemy) in battleContext.battle.opponents.withIndex()) {
		if (enemy == null) continue
		val region = Rectangle(
			minX = region.minX + index * region.width / 4, minY = region.minY,
			width = region.width / 4, height = region.height / 12
		)
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

	renderCombatantInfoPopup(
		battleContext, lateColorBatch, lateKimBatch,
		lateImageBatch, lateTextBatch, lateAnimationPartBatch, Rectangle(
		region.minX, region.minY + region.height / 8,
		region.width, region.boundY - region.height / 8 - region.height / 16,
	))

	val finishColorBatch = context.addColorBatch(2)
	val finishTextBatch = context.addFancyTextBatch(50)
	renderBattleFinishEffect(battleContext, finishColorBatch, finishTextBatch, region)

	return Pair(colorBatch, textBatch)
}

internal fun computeActionBarHeight(regionHeight: Int) = regionHeight / 12
