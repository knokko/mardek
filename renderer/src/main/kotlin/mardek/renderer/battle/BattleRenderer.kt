package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.InGameRenderContext
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.min
import kotlin.math.roundToInt

class BattleRenderer(context: InGameRenderContext, battleState: BattleState) {

	private val context = BattleRenderContext(battleState, context)

	private val turnOrderRenderer = TurnOrderRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12,
		width = context.targetImage.width, height = context.targetImage.height / 12
	))
	private val actionBarRenderer = ActionBarRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height - context.targetImage.height / 12 - context.targetImage.height / 8,
		width = context.targetImage.width, height = context.targetImage.height / 12
	))
	private val skillOrItemSelectionRenderer = SkillOrItemSelectionRenderer(this.context, AbsoluteRectangle(
		minX = context.targetImage.width / 3, minY = context.targetImage.height / 5,
		width = context.targetImage.width / 3, height = 4 * context.targetImage.height / 7
	))
	private val skillOrItemDescriptionRenderer = SkillOrItemDescriptionRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12,
		width = context.targetImage.width, height = context.targetImage.height / 9
	))
	private val targetSelectionRenderer = TargetSelectionRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12, width = context.targetImage.width,
		height = context.targetImage.height - context.targetImage.height / 8 - context.targetImage.height / 12
	))
	private val enemyBlockRenderers = mutableListOf<MonsterBlockRenderer>()
	private val playerBlockRenderers = mutableListOf<PlayerBlockRenderer>()
	private val currentMoveBarRenderer = CurrentMoveBarRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height / 12,
		width = context.targetImage.width, height = context.targetImage.height / 16
	))
	private val challengeBarRenderer = ChallengeBarRenderer(this.context, AbsoluteRectangle(
		minX = 0, minY = context.targetImage.height - context.targetImage.height / 16 - context.targetImage.height / 8,
		width = context.targetImage.width, height = context.targetImage.height / 16
	))
	private val indicatorRenderers = mutableListOf<DamageIndicatorRenderer>()
	private val creatureRenderer = BattleCreatureRenderers(this.context)
	private val particleRenderer = ParticleRenderer(this.context)

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
				minX = index * context.targetImage.width / 4, minY = 0,
				width = context.targetImage.width / 4, height = context.targetImage.height / 12
			)
			indicatorRenderers.add(DamageIndicatorRenderer(context, enemy))
			if (enemy is MonsterCombatantState) enemyBlockRenderers.add(MonsterBlockRenderer(context, enemy, region))
			else playerBlockRenderers.add(PlayerBlockRenderer(context, enemy as PlayerCombatantState, region))
		}

		for ((index, player) in context.battle.players.withIndex()) {
			if (player == null) continue
			val region = AbsoluteRectangle(
				minX = index * context.targetImage.width / 4,
				minY = context.targetImage.height - context.targetImage.height / 8,
				width = context.targetImage.width / 4, height = context.targetImage.height / 8
			)
			indicatorRenderers.add(DamageIndicatorRenderer(context, player))
			if (player is PlayerCombatantState) playerBlockRenderers.add(PlayerBlockRenderer(context, player, region))
			else enemyBlockRenderers.add(MonsterBlockRenderer(context, player as MonsterCombatantState, region))
		}

		for (indicatorRenderer in indicatorRenderers) indicatorRenderer.beforeRendering()
		for (blockRenderer in enemyBlockRenderers) blockRenderer.beforeRendering()
		for (blockRenderer in playerBlockRenderers) blockRenderer.beforeRendering()
	}

	fun render() {
		context.resources.partRenderer.startBatch(context.recorder)
		creatureRenderer.render()
		particleRenderer.render()
		context.resources.partRenderer.endBatch()
		context.uiRenderer.beginBatch()
		val leftColor = srgbToLinear(rgba(90, 76, 44, 200))
		val rightColor = srgbToLinear(rgba(38, 28, 17, 200))
		context.uiRenderer.fillColor(
			0, 0, context.targetImage.width, context.targetImage.height / 12, 0, Gradient(
				0, 0, context.targetImage.width, context.targetImage.height, leftColor, rightColor, leftColor
			)
		)
		context.uiRenderer.fillColor(
			0, context.targetImage.height - context.targetImage.height / 8,
			context.targetImage.width, context.targetImage.height, 0, Gradient(
				0, 0, context.targetImage.width, context.targetImage.height, rightColor, leftColor, rightColor
			)
		)
		context.uiRenderer.endBatch()

		turnOrderRenderer.render()
		actionBarRenderer.render()
		currentMoveBarRenderer.render()
		challengeBarRenderer.render()
		skillOrItemSelectionRenderer.render()
		skillOrItemDescriptionRenderer.render()
		targetSelectionRenderer.render()
		for (indicatorRenderer in indicatorRenderers) indicatorRenderer.render()
		for (blockRenderer in enemyBlockRenderers) blockRenderer.render()
		for (blockRenderer in playerBlockRenderers) blockRenderer.render()

		val state = context.battle.state
		if (state is BattleStateMachine.GameOver) {
			val spentTime = System.nanoTime() - state.startTime
			val fade = min(255L, 255L * spentTime / BattleStateMachine.GameOver.FADE_DURATION).toInt()
			if (fade > 0) {
				context.uiRenderer.beginBatch()
				context.uiRenderer.fillColor(
					0, 0, context.targetImage.width,
					context.targetImage.height, rgba(0, 0, 0, fade)
				)
				context.uiRenderer.endBatch()
			}
		}
		if (state is BattleStateMachine.Victory) {
			val spentTime = System.nanoTime() - state.startTime
			val time1 = 500_000_000L
			if (spentTime > time1) {
				var outlineColor = srgbToLinear(rgb(108, 89, 43))

				val appearDuration = 250_000_000L
				val time2 = time1 + appearDuration
				val fadeBackDuration = 750_000_000L
				val time3 = time2 + fadeBackDuration
				val (a, b) = if (spentTime <= time2) {
					val both = (spentTime - time1).toFloat() / appearDuration.toFloat()
					Pair(both, both)
				} else if (spentTime <= time3) {
					Pair(1f, 1f - (spentTime - time2).toFloat() / fadeBackDuration.toFloat())
				} else Pair(1f, 0f)

				outlineColor = rgba(
					normalize(red(outlineColor)) * (1f - b) + b,
					normalize(green(outlineColor)) * (1f - b) + b,
					normalize(blue(outlineColor)) * (1f - b) + b,
					a
				)
				val innerColor = srgbToLinear(rgba(253, 238, 170, (255 * a).roundToInt()))
				val outerColor = srgbToLinear(rgba(195, 131, 32, (255 * a).roundToInt()))
				context.uiRenderer.beginBatch()

				val width = context.targetImage.width
				val height = context.targetImage.height
				val heightA = height / 12
				// TODO use outlineColor after text render rework
				context.uiRenderer.drawString(
					context.resources.font, "VICTORY!!", innerColor, IntArray(0),
					0, 0, width, height, height / 2, heightA, 2, TextAlignment.CENTER,
					Gradient(
						0, height / 2 - 10 * heightA / 9, width, heightA / 2,
						innerColor, innerColor, outerColor
					),
					Gradient(
						0, height / 2 - heightA / 2, width, 11 * heightA / 5,
						outerColor, outerColor, innerColor
					)
				)
				context.uiRenderer.endBatch()
			}
		}
	}
}
