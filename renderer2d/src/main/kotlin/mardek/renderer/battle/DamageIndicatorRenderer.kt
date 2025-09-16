package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.DamageIndicatorMana
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val DURATION = 2_000_000_000L

internal fun renderDamageIndicator(
	battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch,
	textBatch: MardekGlyphBatch, combatant: CombatantState
) {
	val indicator = combatant.renderInfo.lastDamageIndicator ?: return
	val position = combatant.renderInfo.hitPoint
	val opacity = 1f - (System.nanoTime() - indicator.time) / DURATION.toFloat()
	if (opacity <= 0f) return

	battleContext.run {
		val element = when (indicator) {
			is DamageIndicatorHealth -> indicator.element
			is DamageIndicatorMana -> indicator.element
			else -> null
		}
		if (element != null) {
			val scale = 0.1f * imageBatch.height / element.thickSprite.height
			val size = scale * element.thickSprite.width
			imageBatch.simpleScale(
				position.x - size * 0.5f, position.y - size * 0.5f,
				scale, element.thickSprite.index,
			)
		}

		if (indicator is DamageIndicatorHealth || indicator is DamageIndicatorMana) {
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
			midColor = changeAlpha(srgbToLinear(midColor), (sqrt(opacity) * 255f).roundToInt())
			edgeColor = changeAlpha(srgbToLinear(edgeColor), (sqrt(opacity) * 255f).roundToInt())

			val height = imageBatch.height / 25f
			val unknownFont = context.bundle.getFont(context.content.fonts.basic2.index)
			// TODO Check this
			textBatch.drawFancyString(
				textAmount.toString(), position.x, position.y + height * 0.5f, height,
				unknownFont, edgeColor, rgb(0, 0, 0), height * 0.05f,
				TextAlignment.CENTERED, edgeColor, midColor, midColor, edgeColor,
				0.2f, 0.2f, 0.8f, 0.8f,
			)
//			context.uiRenderer.drawString(
//				context.resources.font, textAmount.toString(), edgeColor, IntArray(0),
//				midX - 5 * height, midY - 5 * height, midX + 5 * height, midY + 5 * height,
//				midY + height / 2, height, 1, TextAlignment.CENTER, Gradient(
//					0, 19 * height / 4, 10 * height, 2 * height / 3, midColor, midColor, midColor
//				)
//			)
		}
	}
}
