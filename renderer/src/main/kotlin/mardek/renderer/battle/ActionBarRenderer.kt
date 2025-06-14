package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.CircleGradient
import com.github.knokko.ui.renderer.Gradient
import mardek.content.sprite.KimSprite
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.*
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.roundToInt

class ActionBarRenderer(
	private val context: BattleRenderContext,
	private val region: AbsoluteRectangle,
) {

	private val state = context.battle.state
	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch

	private val marginY = region.height / 15
	private val marginX = 3 * marginY
	private val iconPositions = IntArray(5)
	private val highDashX = region.minX + 2 * region.width / 3
	private val lowDashX = highDashX - region.height

	private val selectedIndex = if (state !is BattleStateMachine.SelectMove) -1 else when (state.selectedMove) {
		is BattleMoveSelectionAttack -> 0
		is BattleMoveSelectionSkill -> 1
		is BattleMoveSelectionItem -> 2
		BattleMoveSelectionWait -> 3
		BattleMoveSelectionFlee -> 4
	}

	private fun isTargeting(): Boolean {
		if (state !is BattleStateMachine.SelectMove) return false
		val selectedMove = state.selectedMove
		if (selectedMove is BattleMoveSelectionAttack) return selectedMove.target != null
		if (selectedMove is BattleMoveSelectionSkill) return selectedMove.target != null
		if (selectedMove is BattleMoveSelectionItem) return selectedMove.target != null
		return false
	}

	private fun shouldRender() = state is BattleStateMachine.SelectMove && !isTargeting()

	private fun renderIcon(icon: KimSprite, x: Int) {
		val request = KimRequest(
			x = x,
			y = region.minY + marginY,
			scale = (region.height.toFloat() - 2f * marginY) / icon.height,
			sprite = icon,
			opacity = 1f
		)
		if (icon.version == 1) batch1.requests.add(request)
		else batch2.requests.add(request)
	}

	fun beforeRendering() {
		if (!shouldRender()) return

		batch1 = context.resources.kim1Renderer.startBatch()
		batch2 = context.resources.kim2Renderer.startBatch()

		val state = this.state as BattleStateMachine.SelectMove
		val player = state.onTurn
		renderIcon(player.element.sprite, region.maxX - region.height - marginX)

		run {
			var x = lowDashX - region.height
			for (index in 0 until 5) {
				if (index == selectedIndex) x -= region.width / 5
				iconPositions[index] = x
				x -= region.height + marginX
			}

			renderIcon(player.getEquipment(context.updateContext)[0]!!.sprite, iconPositions[0])
			renderIcon(player.player.characterClass.skillClass.icon, iconPositions[1])
			renderIcon(context.content.ui.consumableIcon, iconPositions[2])
			renderIcon(context.content.ui.waitIcon, iconPositions[3])
			renderIcon(context.content.ui.fleeIcon, iconPositions[4])
		}

		var showPointer = true
		val selectedMove = state.selectedMove
		if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) showPointer = false
		if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) showPointer = false
		if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) showPointer = false

		if (showPointer) {
			val pointerScale = region.height.toFloat() / context.content.ui.verticalPointer.height
			batch1.requests.add(KimRequest(
				x = iconPositions[selectedIndex] + region.height / 2 - marginY - (pointerScale * context.content.ui.verticalPointer.width / 2).roundToInt(),
				y = region.minY - 4 * region.height / 5,
				scale = pointerScale,
				sprite = context.content.ui.verticalPointer,
				opacity = 1f
			))
		}
	}

	fun render() {
		if (!shouldRender()) return

		val lineWidth = max(1, region.height / 30)
		val lineColor = srgbToLinear(rgb(208, 193, 142))
		val textColor = srgbToLinear(rgb(238, 203, 117))
		val textOutline = intArrayOf(textColor, rgba(0, 0, 0, 200))
		context.uiRenderer.beginBatch()
		context.uiRenderer.fillColorUnaligned(
			region.minX, region.maxY, lowDashX, region.maxY,
			highDashX, region.minY, region.minX, region.minY,
			rgba(0, 0, 0, 100)
		)

		val player = (state as BattleStateMachine.SelectMove).onTurn.player
		val circleColor = srgbToLinear(rgb(89, 69, 46))
		for (x in iconPositions) {
			if (x == iconPositions[selectedIndex]) {
				val minX = x + region.height / 2
				val width = region.width / 3
				val gradientColor = rgba(red(lineColor), green(lineColor), blue(lineColor), 35.toByte())
				context.uiRenderer.fillColor(
					minX, region.minY + marginY, minX + width, region.maxY - marginY, 0,
					Gradient(0, 0, width, region.height, gradientColor, 0, gradientColor)
				)
			}
			context.uiRenderer.fillCircle(
				x, region.minY + marginY, x + region.height - 2 * marginY, region.maxY - marginY,
				circleColor, CircleGradient(0.85f, 1f, circleColor, lineColor)
			)

			if (x == iconPositions[selectedIndex]) {
				context.uiRenderer.fillCircle(
					x, region.minY + marginY, x + region.height - 2 * marginY, region.maxY - marginY,
					rgba(0, 50, 255, 100)
				)
				val text = when (selectedIndex) {
					0 -> "Attack"
					1 -> player.characterClass.skillClass.name
					2 -> "Items"
					3 -> "Wait"
					4 -> "Flee"
					else -> throw Error("Unexpected selectedIndex $selectedIndex")
				}
				context.uiRenderer.drawString(
					context.resources.font, text, textColor, IntArray(0),
					x + region.height, region.minY, x + region.height + region.width / 5, region.maxY,
					region.maxY - region.height / 4, 4 * region.height / 9, 1, TextAlignment.LEFT
				)
			}
		}

		context.uiRenderer.fillColorUnaligned(
			lowDashX, region.maxY, region.maxX, region.maxY,
			region.maxX, region.minY, highDashX, region.minY,
			srgbToLinear(rgb(82, 62, 37))
		)
		context.uiRenderer.fillColorUnaligned(
			lowDashX, region.maxY - marginY, region.maxX, region.maxY - marginY,
			region.maxX, region.minY + marginY, highDashX + marginX - 2 * marginY, region.minY + marginY,
			0, Gradient(region.minX + region.width / 2, region.minY, region.width, region.height, 0, player.element.color, 0)
		)
		context.uiRenderer.drawString(
			context.resources.font, player.name, textColor, textOutline,
			highDashX, region.minY, region.maxX - region.height - 3 * marginX, region.maxY,
			region.maxY - region.height / 3, region.height / 2, 1, TextAlignment.RIGHT
		)
		context.uiRenderer.fillColor(region.minX, region.minY, region.maxX, region.minY + lineWidth - 1, lineColor)
		context.uiRenderer.fillColor(region.minX, 1 + region.maxY - lineWidth, region.maxX, region.maxY, lineColor)
		context.uiRenderer.fillColorUnaligned(
			lowDashX, region.maxY, lowDashX + 3 * lineWidth - 1, region.maxY,
			highDashX + 3 * lineWidth - 1, region.minY, highDashX, region.minY, lineColor
		)
		context.uiRenderer.endBatch()

		context.resources.kim1Renderer.submit(batch1, context.recorder, context.targetImage)
		context.resources.kim2Renderer.submit(batch2, context.recorder, context.targetImage)
	}
}
