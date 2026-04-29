package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.DamageIndicatorMana
import mardek.state.ingame.battle.DamageIndicatorMiss
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val DURATION = 2_000_000_000L

internal fun renderDamageIndicator(
	battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch,
	textBatch: Vk2dFancyTextBatch, combatant: CombatantState
) {
	val indicator = combatant.renderInfo.lastDamageIndicator ?: return
	val position = combatant.renderInfo.hitPoint

	val font = battleContext.context.bundle.getFont(
		battleContext.context.content.fonts.basic1.index
	)

	if (indicator is DamageIndicatorHealth || indicator is DamageIndicatorMana) {
		val opacity = 1f - (System.nanoTime() - indicator.time) / DURATION.toFloat()
		if (opacity <= 0f) return
		val intOpacity = (sqrt(opacity) * 255f).roundToInt()
		if (intOpacity <= 0) return

		val element = when (indicator) {
			is DamageIndicatorHealth -> indicator.element
			is DamageIndicatorMana -> indicator.element
			else -> null
		}
		if (element != null) {
			val scale = 0.1f * imageBatch.height / element.thickSprite.height
			val size = scale * element.thickSprite.width
			imageBatch.coloredScale(
				position.x - size * 0.5f, position.y - size * 0.5f,
				scale, element.thickSprite.index,
				0, rgba(255, 255, 255, intOpacity),
			)
		}

		val textAmount = if (indicator is DamageIndicatorHealth) abs(indicator.gainedHealth)
		else abs((indicator as DamageIndicatorMana).gainedMana)

		var (midColor, edgeColor) = if (indicator is DamageIndicatorHealth) {
			if (indicator.gainedHealth <= 0) Pair(rgb(232, 222, 210), rgb(180, 154, 110))
			else Pair(rgb(208, 255, 138), rgb(128, 231, 58))
		} else {
			if ((indicator as DamageIndicatorMana).gainedMana >= 0) {
				Pair(rgb(199, 255, 255), rgb(119, 238, 255))
			} else Pair(rgb(255, 170, 255), rgb(182, 90, 192))
		}
		midColor = changeAlpha(srgbToLinear(midColor), intOpacity)
		edgeColor = changeAlpha(srgbToLinear(edgeColor), intOpacity)

		textBatch.drawString(
			textAmount.toString(), position.x, position.y + imageBatch.height * 0.018f,
			0f, imageBatch.height * 0.035f, font,
			MardekTextStyles.BattleIndicators.base(edgeColor, midColor), TextAlignment.CENTERED,
		)
	}

	if (indicator is DamageIndicatorMiss) {
		val jumpDuration = 250_000_000L
		val fadeDuration = 750_000_000L
		val passedTime = System.nanoTime() - indicator.time

		val baseOffsetY = 0.04f * imageBatch.height
		val (opacity, offsetY) = if (passedTime <= jumpDuration) {
			val relativeTime = passedTime.toFloat() / jumpDuration
			val fromMidTime = 0.5f - relativeTime
			Pair(1f, baseOffsetY + (0.25f - fromMidTime.pow(2)) * 0.05f * imageBatch.height)
		} else {
			Pair(1f - (passedTime - jumpDuration).toFloat() / fadeDuration, baseOffsetY)
		}

		if (opacity <= 0f) return
		val intOpacity = (255 * opacity).roundToInt()
		if (intOpacity <= 0) return

		textBatch.drawString(
			"Miss", position.x, position.y - offsetY,
			0f, 0.035f * imageBatch.height, font,
			MardekTextStyles.BattleIndicators.miss(intOpacity), TextAlignment.CENTERED,
		)
	}
}
