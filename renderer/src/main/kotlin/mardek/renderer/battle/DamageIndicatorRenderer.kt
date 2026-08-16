package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.ingame.battle.combatant.CombatantState
import mardek.state.ingame.battle.combatant.DamageIndicatorHistory

internal fun renderDamageIndicator(
	battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch,
	textBatch: Vk2dFancyTextBatch, combatant: CombatantState
) {
	val position = combatant.renderInfo.hitPoint
	val font = battleContext.context.bundle.getFont(
		battleContext.context.content.fonts.basic1.index
	)

	for (indicator in combatant.renderInfo.indicatorHistory.get(battleContext.context.timing)) {
		val element = indicator.element
		val offsetY = imageBatch.height * (-0.01f - 0.03f * indicator.relativeY)
		val textScaleX = 1f + 2.5f * (1f - indicator.heightFactor)
		if (element != null) {
			val scale = 0.1f * imageBatch.height / element.mediumSprite.height
			val size = scale * element.mediumSprite.width
			val radiusX = 0.5f * size * (1f + 1.25f * (1f - indicator.heightFactor))
			val radiusY = 0.5f * size * indicator.heightFactor
			imageBatch.colored(
				position.x - radiusX, position.y + offsetY - radiusY,
				position.x + radiusX, position.y + offsetY + radiusY,
				element.mediumSprite.index, 0,
				rgba(1f, 1f, 1f, indicator.opacity),
			)
		}

		var (midColor, edgeColor) = when (indicator.type) {
			DamageIndicatorHistory.ResultType.GainHealth -> Pair(
				rgb(208, 255, 138), rgb(128, 231, 58)
			)
			DamageIndicatorHistory.ResultType.GainMana -> Pair(
				rgb(199, 255, 255), rgb(119, 238, 255)
			)
			DamageIndicatorHistory.ResultType.LoseMana -> Pair(
				rgb(255, 170, 255), rgb(182, 90, 192)
			)
			else -> Pair(
				rgb(232, 222, 210), rgb(180, 154, 110)
			)
		}
		midColor = changeAlpha(srgbToLinear(midColor), indicator.opacity)
		edgeColor = changeAlpha(srgbToLinear(edgeColor), indicator.opacity)

		val text = if (indicator.type == DamageIndicatorHistory.ResultType.Miss) "Miss" else indicator.amount.toString()
		textBatch.drawString(
			text, position.x, position.y + imageBatch.height * 0.018f * indicator.heightFactor + offsetY,
			0f, imageBatch.height * 0.035f * indicator.heightFactor, font,
			MardekTextStyles.BattleIndicators.base(edgeColor, midColor, indicator.opacity),
			TextAlignment.CENTERED, textScaleX
		)
	}
}
