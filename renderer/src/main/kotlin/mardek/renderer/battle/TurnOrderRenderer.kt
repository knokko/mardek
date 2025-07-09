package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dKimBatch
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.TurnOrderSimulator
import mardek.state.util.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt

fun renderTurnOrder(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	kimBatch: Vk2dKimBatch, textBatch: Vk2dGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val slotWidth = region.height
		val triangleWidth = region.height / 4
		val midY = region.minY + region.height / 2

		val stateMachine = battle.state
		val onTurn = when (stateMachine) {
			is BattleStateMachine.MeleeAttack -> stateMachine.attacker
			is BattleStateMachine.CastSkill -> stateMachine.caster
			is BattleStateMachine.UseItem -> stateMachine.thrower
			is BattleStateMachine.SelectMove -> stateMachine.onTurn
			else -> null
		}

		if (slotWidth < 5) return
		if (stateMachine is BattleStateMachine.MeleeAttack && stateMachine.skill != null) return
		if (stateMachine is BattleStateMachine.CastSkill) return
		if (stateMachine is BattleStateMachine.UseItem) return
		if (stateMachine is BattleStateMachine.SelectMove) {
			val selectedMove = stateMachine.selectedMove
			if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) return
			if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) return
			if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) return
		}
		if (battle.livingPlayers().isEmpty() || battle.livingOpponents().isEmpty()) return

		val scale = region.height / 24f
		val spriteSize = (16 * scale).roundToInt()

		val lineWidth = max(1, region.height / 20)
		var x = region.minX

		val lineColor = srgbToLinear(rgb(208, 193, 142))
		val backgroundColor = srgbToLinear(rgb(25, 13, 9))
		colorBatch.fillUnaligned(
			x, region.minY, x + slotWidth, region.minY,
			x + slotWidth + triangleWidth, midY, x, midY, backgroundColor
		)
		colorBatch.fillUnaligned(
			x, region.maxY, x + slotWidth, region.maxY,
			x + slotWidth + triangleWidth, midY, x, midY, backgroundColor
		)
		x += slotWidth

		val simulator = TurnOrderSimulator(battle, updateContext)
		val darkPlayerColor = srgbToLinear(rgba(49, 84, 122, 200))
		val lightPlayerColor = srgbToLinear(rgba(89, 118, 148, 200))
		val darkEnemyColor = srgbToLinear(rgba(131, 45, 32, 200))
		val lightEnemyColor = srgbToLinear(rgba(155, 87, 84, 200))
		val onTurnColor = run {
			val period = 1_500_000_000L
			val relative = (System.nanoTime() - battle.startTime) % period
			val intensity = cos(relative * 2 * PI / period)
			rgba(200, 200, 50, (60 + 50 * intensity).roundToInt())
		}

		var isFirst = true
		while (x < region.maxX) {
			val combatant = if (isFirst && onTurn != null) {
				onTurn
			} else {
				simulator.checkReset()
				simulator.next() ?: break
			}

			val (lightColor, darkColor) = if (combatant.isOnPlayerSide) Pair(lightPlayerColor, darkPlayerColor)
			else Pair(lightEnemyColor, darkEnemyColor)

			val minTriX = x + triangleWidth
			val maxTriX = minTriX + slotWidth
			colorBatch.fillUnaligned(
				x, region.minY, x + slotWidth, region.minY,
				maxTriX, midY, minTriX, midY, darkColor
			)
			colorBatch.fillUnaligned(
				x + 3 * lineWidth, region.minY + 2 * lineWidth,
				x + slotWidth, region.minY + 2 * lineWidth,
				maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, lightColor
			)
			colorBatch.fillUnaligned(
				x, region.maxY, x + slotWidth, region.maxY,
				maxTriX, midY, minTriX, midY, darkColor
			)
			if (isFirst && onTurn != null) {
				colorBatch.fillUnaligned(
					x + 3 * lineWidth, region.minY + 2 * lineWidth,
					x + slotWidth, region.minY + 2 * lineWidth,
					maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, onTurnColor
				)
				colorBatch.fillUnaligned(
					x + 3 * lineWidth, region.maxY - 2 * lineWidth,
					x + slotWidth, region.maxY - 2 * lineWidth,
					maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, onTurnColor
				)
			}
			colorBatch.fillUnaligned(
				x, region.minY, x + lineWidth, region.minY,
				minTriX + lineWidth, midY, minTriX, midY, lineColor
			)
			colorBatch.fillUnaligned(
				x, region.maxY, x + lineWidth, region.maxY,
				minTriX + lineWidth, midY, minTriX, midY, lineColor
			)

			kimBatch.simple(
				x + slotWidth - spriteSize,
				region.minY + (region.height - spriteSize) / 2,
				scale, combatant.getTurnOrderIcon().index
			)

			x += slotWidth
			isFirst = false
		}

		colorBatch.fill(
			region.minX, region.minY,
			region.maxX, region.minY + lineWidth - 1, lineColor
		)
		colorBatch.fill(
			region.minX, 1 + region.maxY - lineWidth,
			region.maxX, region.maxY, lineColor
		)

		x = region.minX

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		textBatch.drawString(
			"TURN", x + slotWidth * 0.1f, region.minY + 0.4f * region.height,
			0.2f * region.height, font, lineColor
		)
		textBatch.drawString(
			"ORDER", x + slotWidth * 0.1f, region.minY + 0.75f * region.height,
			0.2f * region.height, font, lineColor
		)
	}
}
