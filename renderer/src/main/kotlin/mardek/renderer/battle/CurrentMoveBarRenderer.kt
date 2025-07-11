package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.title.AbsoluteRectangle

class CurrentMoveBarRenderer(private val context: BattleRenderContext, private val region: AbsoluteRectangle) {

	private val state = context.battle.state
	private val currentSkill = when (state) {
		is BattleStateMachine.MeleeAttack -> state.skill
		is BattleStateMachine.CastSkill -> state.skill
		else -> null
	}
	private val currentItem = if (state is BattleStateMachine.UseItem) state.item else null
	private lateinit var batch: KimBatch

	fun beforeRendering() {
		if (currentSkill == null && currentItem == null) return
		batch = context.resources.kim2Renderer.startBatch()
		if (currentSkill != null) {
			batch.requests.add(KimRequest(
				x = region.minX + region.width / 4, y = region.minY,
				scale = region.height.toFloat() / currentSkill.element.sprite.height,
				sprite = currentSkill.element.sprite
			))
		}
		if (currentItem != null) {
			batch.requests.add(KimRequest(
				x = region.minX + region.width / 4, y = region.minY,
				scale = region.height.toFloat() / currentItem.sprite.height,
				sprite = currentItem.sprite
			))
		}
	}

	fun render() {
		if (currentSkill == null && currentItem == null) return

		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context, 4)
		val lightBottomColor = srgbToLinear(rgba(80, 65, 55, 220))
		val lightTopColor = srgbToLinear(rgba(120, 110, 110, 220))
		val lightRightColor = srgbToLinear(rgba(130, 110, 70, 220))
		val darkLeftColor = srgbToLinear(rgba(38, 32, 32, 220))
		val darkRightColor = srgbToLinear(rgba(100, 90, 50, 220))
		val midY = region.minY + region.height / 2
		val borderHeight = region.height / 10
		rectangles.fill(
			region.minX, region.minY, region.maxX, region.minY + borderHeight - 1,
			srgbToLinear(rgb(208, 193, 142)),
		)
		rectangles.fill(
			region.minX, region.maxY - borderHeight, region.maxX, region.maxY,
			srgbToLinear(rgb(208, 193, 142)),
		)
		rectangles.gradient(
			region.minX, region.minY + borderHeight, region.maxX, midY - 1,
			lightBottomColor, lightRightColor, lightTopColor
		)
		rectangles.gradient(
			region.minX, midY, region.maxX, region.maxY - borderHeight,
			darkLeftColor, darkRightColor, darkLeftColor
		)
		rectangles.endBatch(context.recorder)

		val textX = region.minX + region.width / 4 + 7 * region.height / 6
		val textColor = srgbToLinear(rgb(238, 203, 127))
		val name = currentSkill?.name ?: currentItem!!.flashName
		context.uiRenderer.beginBatch()
		context.uiRenderer.drawString(
			context.resources.font, name, textColor, IntArray(0),
			textX, region.minY, region.maxX, region.maxY,
			region.maxY - region.height / 4, 4 * region.height / 9, 1, TextAlignment.LEFT
		)
		context.uiRenderer.endBatch()

		if (currentSkill != null) {
			context.resources.kim2Renderer.submit(batch, context)
		} else {
			context.resources.kim1Renderer.submit(batch, context)
		}
	}
}
