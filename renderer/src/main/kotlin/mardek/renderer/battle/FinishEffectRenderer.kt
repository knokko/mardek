package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.state.ingame.battle.BattleStateMachine
import kotlin.math.min
import kotlin.math.roundToInt

class FinishEffectRenderer(private val context: BattleRenderContext) {

	fun render() {
		val state = context.battle.state
		if (state is BattleStateMachine.GameOver) {
			val spentTime = System.nanoTime() - state.startTime
			val fade = min(255L, 255L * spentTime / BattleStateMachine.GameOver.FADE_DURATION).toInt()
			if (fade > 0) {
				val rectangles = context.resources.rectangleRenderer
				rectangles.beginBatch(context.recorder, context.targetImage, 1)
				rectangles.fill(
					0, 0, context.targetImage.width, context.targetImage.height,
					rgba(0, 0, 0, fade)
				)
				rectangles.endBatch(context.recorder)
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
