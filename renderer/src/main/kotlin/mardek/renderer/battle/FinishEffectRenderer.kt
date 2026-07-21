package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal fun renderBattleFinishEffect(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	textBatch: Vk2dFancyTextBatch, region: Rectangle
) {
	battleContext.run {
		val stateMachine = battle.state
		if (stateMachine is BattleStateMachine.GameOver) {
			val fadeAlpha = context.timing.interpolate(
				stateMachine.startTime, 0,
				BattleStateMachine.GameOver.FADE_DURATION, 255, true
			)
			if (fadeAlpha > 0) {
				colorBatch.fill(
					region.minX, region.minY, region.maxX, region.maxY,
					rgba(0, 0, 0, fadeAlpha)
				)
			}
		}
		if (stateMachine is BattleStateMachine.Victory) {
			val spentTime = context.timing.elapsedTimeSince(stateMachine.startTime)
			val time1 = 500.milliseconds
			if (spentTime > time1 && !stateMachine.shouldGoToLootMenu(context.campaign.time)) {
				var strokeColor = srgbToLinear(rgb(108, 89, 43))

				val appearDuration = 250.milliseconds
				val time2 = time1 + appearDuration
				val fadeBackDuration = 750.milliseconds
				val time3 = time2 + fadeBackDuration
				val (a, b) = if (spentTime <= time2) {
					val both = ((spentTime - time1) / appearDuration).toFloat()
					Pair(both, both)
				} else if (spentTime <= time3) {
					Pair(1f, 1f - ((spentTime - time2) / fadeBackDuration).toFloat())
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
				for (style in arrayOf(
					MardekTextStyles.victoryBack(strokeColor),
					MardekTextStyles.victoryFront(innerColor, outerColor),
				)) {
					textBatch.drawString(
						"VICTORY!!", region.width * 0.5f, region.height * 0.5f, 0f,
						region.height / 12f, victoryFont, style, TextAlignment.CENTERED,
					)
				}
			}
		}
	}
}
