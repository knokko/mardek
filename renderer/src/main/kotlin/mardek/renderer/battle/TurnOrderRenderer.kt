package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.TurnOrderSimulator
import mardek.state.title.AbsoluteRectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt

class TurnOrderRenderer(
	private val battle: BattleState,
	private val resources: SharedResources,
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	frameIndex: Int,
	private val region: AbsoluteRectangle,
) {
	private val slotWidth = region.height
	private val triangleWidth = region.height / 4
	private val midY = region.minY + region.height / 2

	private val onTurn = battle.onTurn
	private val renderer = resources.uiRenderers[frameIndex]
	private lateinit var kimBatch: KimBatch

	fun beforeRendering() {
		if (slotWidth < 5) return
		kimBatch = resources.kim1Renderer.startBatch()

		val scale = region.height / 24f
		val spriteSize = (16 * scale).roundToInt()

		var x = region.minX + slotWidth
		var isFirst = true
		val simulator = TurnOrderSimulator(battle)
		while (x + slotWidth < region.maxX) {
			val combatant = if (isFirst && onTurn != null) {
				onTurn
			} else {
				simulator.checkReset()
				simulator.next() ?: break
			}
			val sprite = if (combatant.isPlayer) {
				battle.players[combatant.index]!!.areaSprites.sprites[0]
			} else battle.battle.enemies[combatant.index]!!.monster.type.icon

			kimBatch.requests.add(KimRequest(
				x = x + slotWidth - spriteSize,
				y = region.minY + (region.height - spriteSize) / 2,
				scale = scale, sprite = sprite, opacity = 1f
			))

			x += slotWidth
			isFirst = false
		}
	}

	fun render() {
		if (slotWidth < 5) return

		renderer.beginBatch()

		var x = region.minX
		val lineColor = srgbToLinear(rgb(208, 193, 142))
		val lineWidth = max(1, region.height / 20)
		run {
			val backgroundColor = srgbToLinear(rgb(25, 13, 9))
			renderer.fillColorUnaligned(
				x, region.minY, x + slotWidth, region.minY,
				x + slotWidth + triangleWidth, midY, x, midY, backgroundColor
			)
			renderer.fillColorUnaligned(
				x, region.maxY, x + slotWidth, region.maxY,
				x + slotWidth + triangleWidth, midY, x, midY, backgroundColor
			)
			renderer.drawString(
				resources.font, "TURN", lineColor, IntArray(0),
				x + slotWidth / 10, region.minY, x + slotWidth, region.maxY,
				region.minY + 2 * region.height / 5, region.height / 5, 1, TextAlignment.LEFT
			)
			renderer.drawString(
				resources.font, "ORDER", lineColor, IntArray(0),
				x + slotWidth / 10, region.minY, x + slotWidth, region.maxY,
				region.minY + 3 * region.height / 4, region.height / 5, 1, TextAlignment.LEFT
			)
			x += slotWidth
		}

		val simulator = TurnOrderSimulator(battle)
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
		while (x + slotWidth < region.maxX) {
			val combatant = if (isFirst && onTurn != null) {
				onTurn
			} else {
				simulator.checkReset()
				simulator.next() ?: break
			}

			val (lightColor, darkColor) = if (combatant.isPlayer) Pair(lightPlayerColor, darkPlayerColor)
			else Pair(lightEnemyColor, darkEnemyColor)
			val minTriX = x + triangleWidth
			val maxTriX = minTriX + slotWidth
			renderer.fillColorUnaligned(
				x, region.minY, x + slotWidth, region.minY, maxTriX, midY, minTriX, midY, darkColor
			)
			renderer.fillColorUnaligned(
				x + 3 * lineWidth, region.minY + 2 * lineWidth,
				x + slotWidth, region.minY + 2 * lineWidth,
				maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, lightColor
			)
			renderer.fillColorUnaligned(
				x, region.maxY, x + slotWidth, region.maxY, maxTriX, midY, minTriX, midY, darkColor
			)
			if (isFirst && onTurn != null) {
				renderer.fillColorUnaligned(
					x + 3 * lineWidth, region.minY + 2 * lineWidth,
					x + slotWidth, region.minY + 2 * lineWidth,
					maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, onTurnColor
				)
				renderer.fillColorUnaligned(
					x + 3 * lineWidth, region.maxY - 2 * lineWidth,
					x + slotWidth, region.maxY - 2 * lineWidth,
					maxTriX - lineWidth, midY, minTriX + 2 * lineWidth, midY, onTurnColor
				)
			}
			renderer.fillColorUnaligned(
				x, region.minY, x + lineWidth, region.minY, minTriX + lineWidth, midY, minTriX, midY, lineColor
			)
			renderer.fillColorUnaligned(
				x, region.maxY, x + lineWidth, region.maxY, minTriX + lineWidth, midY, minTriX, midY, lineColor
			)
			x += slotWidth
			isFirst = false
		}

		renderer.fillColor(region.minX, region.minY, region.maxX, region.minY + lineWidth - 1, lineColor)
		renderer.fillColor(region.minX, 1 + region.maxY - lineWidth, region.maxX, region.maxY, lineColor)
		renderer.endBatch()

		resources.kim1Renderer.submit(kimBatch, recorder, targetImage)
	}
}
