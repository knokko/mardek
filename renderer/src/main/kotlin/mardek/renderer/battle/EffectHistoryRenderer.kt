package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.changeAlpha
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.StatusEffectHistory
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class EffectHistoryRenderer(private val context: BattleRenderContext, private val combatant: CombatantState) {

	private val currentEntry = combatant.effectHistory.get(System.nanoTime())
	private val width = context.viewportWidth
	private val height = context.viewportHeight
	private var midX = 0
	private var midY = 0
	private var spriteSize = 0

	private var kimBatch: KimBatch? = null

	fun beforeRendering() {
		if (currentEntry == null) return

		midX = TransformedCoordinates.intX(combatant.lastRenderedPosition.first, width) - height / 20
		midY = TransformedCoordinates.intY(combatant.lastRenderedPosition.second, height)
		if (currentEntry.type == StatusEffectHistory.Type.Remove) {
			midY -= (currentEntry.relativeTime * height / 20).roundToInt()
			spriteSize = height / 20
			val sprite = currentEntry.effect.icon
			kimBatch = context.resources.kim1Renderer.startBatch()
			kimBatch!!.requests.add(KimRequest(
				x = midX - spriteSize / 2, y = midY - spriteSize / 2,
				scale = spriteSize / sprite.height.toFloat(), sprite = sprite,
				opacity = 1f - 4f * (0.5f - currentEntry.relativeTime).pow(2)
			))
		} else {
			val f = sin(4f * currentEntry.relativeTime)
			midY -= (f * height / 20).roundToInt()
		}
	}

	fun render() {
		if (currentEntry == null) return
		kimBatch?.let { context.resources.kim1Renderer.submit(it, context) }

		if (currentEntry.type == StatusEffectHistory.Type.Remove) {
			val crossColor = srgbToLinear(rgba(199, 0, 0, 128))
			val l = height / 28
			val w = height / 250
			val p = (l - 2 * l * (1f - currentEntry.relativeTime)).roundToInt()

			val rectangles = context.resources.rectangleRenderer
			rectangles.beginBatch(context, 2)
			rectangles.fillUnaligned(
				midX - l + w, midY - l - w, midX - l - w, midY - l + w,
				midX + p - w, midY + p + w, midX + p + w, midY + p - w, crossColor
			)
			rectangles.fillUnaligned(
				midX + l - w, midY - l - w, midX + l + w, midY - l + w,
				midX - p + w, midY + p + w, midX - p - w, midY + p - w, crossColor
			)
			rectangles.endBatch(context.recorder)
		} else {
			val f = currentEntry.relativeTime.pow(2)
			val color = changeAlpha(srgbToLinear(currentEntry.effect.textColor), 255 - (250 * f).roundToInt())

			context.uiRenderer.beginBatch()
			context.uiRenderer.drawString(
				context.resources.font, currentEntry.effect.shortName, color, IntArray(0),
				midX - width / 5, 0, midX + width / 5, height,
				midY, height / 30, 1, TextAlignment.CENTER
			)
			context.uiRenderer.endBatch()
		}
	}
}
