package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.renderer.InGameRenderContext
import mardek.renderer.battle.block.MonsterBlockRenderer
import mardek.renderer.battle.block.PlayerBlockRenderer
import mardek.renderer.battle.creature.BattleCreatureRenderers
import mardek.renderer.battle.particle.EffectParticleRenderer
import mardek.renderer.battle.particle.ParticleRenderer
import mardek.renderer.battle.ui.ActionBarRenderer
import mardek.renderer.battle.ui.ChallengeBarRenderer
import mardek.renderer.battle.ui.CombatantInfoModalRenderer
import mardek.renderer.battle.ui.SkillOrItemDescriptionRenderer
import mardek.renderer.battle.ui.SkillOrItemSelectionRenderer
import mardek.renderer.battle.ui.TargetSelectionRenderer
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.title.AbsoluteRectangle

class BattleRenderer(context: InGameRenderContext, battleState: BattleState) {

	private val context = BattleRenderContext(battleState, context)

	private val turnOrderRenderer = TurnOrderRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.viewportHeight / 12,
		width = context.viewportWidth, height = context.viewportHeight / 12
	))
	private val actionBarRenderer = ActionBarRenderer(
		this.context, AbsoluteRectangle(
			minX = 0,
			minY = context.viewportHeight - context.viewportHeight / 12 - context.viewportHeight / 8,
			width = context.viewportWidth,
			height = context.viewportHeight / 12
		)
	)
	private val skillOrItemSelectionRenderer = SkillOrItemSelectionRenderer(
		this.context, AbsoluteRectangle(
			minX = context.viewportWidth / 3, minY = context.viewportHeight / 5,
			width = context.viewportWidth / 3, height = 4 * context.viewportHeight / 7
		)
	)
	private val skillOrItemDescriptionRenderer = SkillOrItemDescriptionRenderer(
		this.context, AbsoluteRectangle(
			minX = 0, minY = context.viewportHeight / 12,
			width = context.viewportWidth, height = context.viewportHeight / 9
		)
	)
	private val targetSelectionRenderer = TargetSelectionRenderer(
		this.context, AbsoluteRectangle(
			minX = 0, minY = context.viewportHeight / 12, width = context.viewportWidth,
			height = context.viewportHeight - context.viewportHeight / 8 - context.viewportHeight / 12
		)
	)
	private val enemyBlockRenderers = mutableListOf<MonsterBlockRenderer>()
	private val playerBlockRenderers = mutableListOf<PlayerBlockRenderer>()
	private val currentMoveBarRenderer = CurrentMoveBarRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.viewportHeight / 12,
		width = context.viewportWidth, height = context.viewportHeight / 16
	))
	private val challengeBarRenderer = ChallengeBarRenderer(
		this.context, AbsoluteRectangle(
			minX = 0,
			minY = context.viewportHeight - context.viewportHeight / 16 - context.viewportHeight / 8,
			width = context.viewportWidth,
			height = context.viewportHeight / 16
		)
	)
	private val indicatorRenderers = mutableListOf<DamageIndicatorRenderer>()
	private val effectRenderers = mutableListOf<EffectHistoryRenderer>()
	private val creatureRenderer = BattleCreatureRenderers(this.context)
	private val effectParticleRenderer = EffectParticleRenderer(this.context)
	private val particleRenderer = ParticleRenderer(this.context)
	private val itemRenderer = ThrownItemRenderer(this.context)
	private val infoRenderer = CombatantInfoModalRenderer(this.context)
	private val finishRenderer = FinishEffectRenderer(this.context)

	fun beforeRendering() {
		turnOrderRenderer.beforeRendering()
		actionBarRenderer.beforeRendering()
		currentMoveBarRenderer.beforeRendering()
		challengeBarRenderer.beforeRendering()
		skillOrItemSelectionRenderer.beforeRendering()
		skillOrItemDescriptionRenderer.beforeRendering()
		targetSelectionRenderer.beforeRendering()

		for ((index, enemy) in context.battle.opponents.withIndex()) {
			if (enemy == null) continue
			val region = AbsoluteRectangle(
				minX = index * context.viewportWidth / 4, minY = 0,
				width = context.viewportWidth / 4, height = context.viewportHeight / 12
			)
			indicatorRenderers.add(DamageIndicatorRenderer(context, enemy))
			effectRenderers.add(EffectHistoryRenderer(context, enemy))
			if (enemy is MonsterCombatantState) enemyBlockRenderers.add(MonsterBlockRenderer(context, enemy, region))
			else playerBlockRenderers.add(PlayerBlockRenderer(context, enemy as PlayerCombatantState, region))
		}

		for ((index, player) in context.battle.players.withIndex()) {
			if (player == null) continue
			val region = AbsoluteRectangle(
				minX = index * context.viewportWidth / 4,
				minY = context.viewportHeight - context.viewportHeight / 8,
				width = context.viewportWidth / 4, height = context.viewportHeight / 8
			)
			indicatorRenderers.add(DamageIndicatorRenderer(context, player))
			effectRenderers.add(EffectHistoryRenderer(context, player))
			if (player is PlayerCombatantState) playerBlockRenderers.add(PlayerBlockRenderer(context, player, region))
			else enemyBlockRenderers.add(MonsterBlockRenderer(context, player as MonsterCombatantState, region))
		}

		for (indicatorRenderer in indicatorRenderers) indicatorRenderer.beforeRendering()
		for (effectRenderer in effectRenderers) effectRenderer.beforeRendering()
		for (blockRenderer in enemyBlockRenderers) blockRenderer.beforeRendering()
		for (blockRenderer in playerBlockRenderers) blockRenderer.beforeRendering()
		itemRenderer.beforeRendering()
		infoRenderer.beforeRendering()
	}

	fun render() {
		context.resources.partRenderer.startBatch(context.recorder)
		creatureRenderer.render()
		effectParticleRenderer.render()
		particleRenderer.render()
		context.resources.partRenderer.endBatch()

		val rectangles = context.resources.rectangleRenderer
		val leftColor = srgbToLinear(rgba(90, 76, 44, 200))
		val rightColor = srgbToLinear(rgba(38, 28, 17, 200))
		rectangles.beginBatch(context, 2)
		rectangles.gradient(
			0, 0, context.viewportWidth - 1, context.viewportHeight / 12,
			leftColor, rightColor, leftColor
		)
		rectangles.gradient(
			0, context.viewportHeight - context.viewportHeight / 8,
			context.viewportWidth, context.viewportHeight,
			rightColor, leftColor, rightColor
		)
		rectangles.endBatch(context.recorder)

		turnOrderRenderer.render()
		actionBarRenderer.render()
		currentMoveBarRenderer.render()
		challengeBarRenderer.render()
		skillOrItemSelectionRenderer.render()
		skillOrItemDescriptionRenderer.render()
		targetSelectionRenderer.render()
		for (indicatorRenderer in indicatorRenderers) indicatorRenderer.render()
		for (effectRenderer in effectRenderers) effectRenderer.render()
		for (blockRenderer in enemyBlockRenderers) blockRenderer.render()
		for (blockRenderer in playerBlockRenderers) blockRenderer.render()
		itemRenderer.render()
		infoRenderer.render()
		finishRenderer.render()
	}
}
