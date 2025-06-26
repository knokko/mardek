package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.changeAlpha
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.DamageIndicatorMana
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val DURATION = 2_000_000_000L

class DamageIndicatorRenderer(
	private val context: BattleRenderContext,
	combatant: CombatantState,
) {

	private val indicator = combatant.lastDamageIndicator
	private var opacity = 0f
	private var midX = 0
	private var midY = 0
	private val position = combatant.lastRenderedPosition
	private lateinit var batch: KimBatch

	fun beforeRendering() {
		if (indicator == null) return
		opacity = 1f - (System.nanoTime() - indicator.time) / DURATION.toFloat()
		if (opacity <= 0f) return

		midX = TransformedCoordinates.intX(position.first, context.targetImage.width)
		midY = TransformedCoordinates.intY(position.second, context.targetImage.height)

		val element = when (indicator) {
			is DamageIndicatorHealth -> indicator.element
			is DamageIndicatorMana -> indicator.element
			else -> null
		}
		if (element != null) {
			batch = context.resources.kim2Renderer.startBatch()
			val scale = 0.1f * context.targetImage.height / element.sprite.height
			val size = (scale * element.sprite.width).roundToInt()
			batch.requests.add(KimRequest(
				x = midX - size / 2, y = midY - size / 2, scale = scale,
				sprite = element.sprite, opacity = opacity * 0.7f
			))
		}
	}

	fun render() {
		if (!this::batch.isInitialized) return

		context.resources.kim2Renderer.submit(batch, context.recorder, context.targetImage)

		context.uiRenderer.beginBatch()

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

			val height = context.targetImage.height / 25
			context.uiRenderer.drawString(
				context.resources.font, textAmount.toString(), edgeColor, IntArray(0),
				midX - 5 * height, midY - 5 * height, midX + 5 * height, midY + 5 * height,
				midY + height / 2, height, 1, TextAlignment.CENTER, Gradient(
					0, 19 * height / 4, 10 * height, 2 * height / 3, midColor, midColor, midColor
				)
			)
		}

		context.uiRenderer.endBatch()
	}
}
