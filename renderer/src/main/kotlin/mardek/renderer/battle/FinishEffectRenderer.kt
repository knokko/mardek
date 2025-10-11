package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderBattleFinishEffect(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	textBatch: MardekGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val stateMachine = battle.state
		if (stateMachine is BattleStateMachine.GameOver) {
			val spentTime = System.nanoTime() - stateMachine.startTime
			val fade = min(255L, 255L * spentTime / BattleStateMachine.GameOver.FADE_DURATION).toInt()
			if (fade > 0) {
				colorBatch.fill(
					region.minX, region.minY, region.maxX, region.maxY,
					rgba(0, 0, 0, fade)
				)
			}
		}
		if (stateMachine is BattleStateMachine.Victory) {
			val spentTime = System.nanoTime() - stateMachine.startTime
			val time1 = 500_000_000L
			if (spentTime > time1 && !stateMachine.shouldGoToLootMenu()) {
				var strokeColor = srgbToLinear(rgb(108, 89, 43))

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

				strokeColor = rgba(
					normalize(red(strokeColor)) * (1f - b) + 0.6f * b,
					normalize(green(strokeColor)) * (1f - b) + 0.5f * b,
					normalize(blue(strokeColor)) * (1f - b) + 0.4f * b,
					a
				)
				val innerColorA = srgbToLinear(rgba(250, 240, 180, (255 * a).roundToInt()))
				val outerColorA = srgbToLinear(rgba(210, 150, 40, (255 * a).roundToInt()))
				val innerColorB = srgbToLinear(rgba(250, 240, 200, (255 * a).roundToInt()))
				val outerColorB = srgbToLinear(rgba(220, 180, 110, (255 * a).roundToInt()))

				var innerColor = innerColorA
				var outerColor = outerColorA
				if (a == 1f && b > 0f) {
					innerColor = interpolateColors(innerColorA, innerColorB, b)
					outerColor = interpolateColors(outerColorA, outerColorB, b)
				}

				val victoryFont = context.bundle.getFont(context.content.fonts.large2.index)
				textBatch.drawFancyString(
					"VICTORY!!", region.width * 0.5f, region.height * 0.5f,
					region.height / 12f, victoryFont, outerColor, strokeColor,
					region.height * 0.01f, TextAlignment.CENTERED,
					outerColor, innerColor, outerColor, outerColor,
					0.2f, 0.5f, 0.8f, 1f,
				)
			}
		}
	}
}
