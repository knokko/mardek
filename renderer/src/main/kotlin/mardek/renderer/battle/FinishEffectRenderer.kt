package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle

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
			val elapsedTime = context.timing.elapsedTimeSince(stateMachine.startTime)
			if (elapsedTime >= BattleStateMachine.Victory.DELAY_UNTIL_TEXT) {

				val alpha = if (elapsedTime >= BattleStateMachine.Victory.DELAY_UNTIL_TEXT_FADE_OUT) {
					context.timing.interpolate(
						stateMachine.startTime.virtualAdd(BattleStateMachine.Victory.DELAY_UNTIL_TEXT_FADE_OUT), 255,
						BattleStateMachine.Victory.VICTORY_TEXT_FADE_OUT, 0, true,
					)
				} else {
					context.timing.interpolate(
						stateMachine.startTime.virtualAdd(BattleStateMachine.Victory.DELAY_UNTIL_TEXT), 0,
						BattleStateMachine.Victory.VICTORY_TEXT_FADE_IN, 255, true
					)
				}

				val brightness = if (elapsedTime >= BattleStateMachine.Victory.DELAY_UNTIL_TEXT_BLINK_OUT) {
					context.timing.interpolate(
						stateMachine.startTime.virtualAdd(BattleStateMachine.Victory.DELAY_UNTIL_TEXT_BLINK_OUT), 1f,
						BattleStateMachine.Victory.VICTORY_TEXT_BLINK_OUT, 0f, true,
					)
				} else {
					context.timing.interpolate(
						stateMachine.startTime.virtualAdd(BattleStateMachine.Victory.DELAY_UNTIL_TEXT), 0f,
						BattleStateMachine.Victory.VICTORY_TEXT_FADE_IN, 1f, true,
					)
				}

				if (alpha > 0) {
					val victoryFont = context.bundle.getFont(context.content.fonts.large2.index)
					for (style in MardekTextStyles.Victory.styles(alpha, brightness)) {
						textBatch.drawString(
							"VICTORY!!", region.width * 0.5f, region.height * 0.5f, 0f,
							region.height / 12f, victoryFont, style, TextAlignment.CENTERED,
						)
					}
				}
			}
		}
	}
}
