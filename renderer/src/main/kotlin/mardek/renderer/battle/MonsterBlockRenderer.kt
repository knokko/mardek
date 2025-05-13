package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MonsterBlockRenderer(
	private val context: BattleRenderContext,
	private val enemy: MonsterCombatantState,
	private val region: AbsoluteRectangle,
) {
	private val currentTime = System.nanoTime()
	private val opacity: Float

	private var nameX = 0
	private lateinit var kimBatch: KimBatch

	init {
		if (!enemy.isAlive()) {
			val lastDamage = enemy.lastDamageIndicator
			if (lastDamage != null) {
				val spentTime = currentTime - lastDamage.time
				val vanishTime = 1_000_000_000L
				opacity = 1f - spentTime.toFloat() / vanishTime.toFloat()
			} else opacity = 0f
		} else opacity = 1f
	}

	fun beforeRendering() {
		if (opacity <= 0f) return
		kimBatch = context.resources.kim2Renderer.startBatch()
		val sprite = enemy.element.sprite
		val marginY = region.height / 20
		val scale = (region.height - 2 * marginY) / sprite.height.toFloat()
		kimBatch.requests.add(KimRequest(
			x = region.minX, y = region.minY + marginY, scale = scale, sprite = sprite, opacity = 1f
		))
		nameX = region.minX + (scale * sprite.width).roundToInt() + region.width / 50
	}

	fun render() {
		if (opacity <= 0f) return
		context.uiRenderer.beginBatch()
		run {
			val marginY = region.height / 10
			val minX = region.minX + region.height / 2
			val minY = region.minY + marginY
			val maxX = minX + 3 * region.width / 4
			val maxY = region.minY + region.height / 2
			val weakColor = rgba(red(enemy.element.color), green(enemy.element.color), blue(enemy.element.color), 150.toByte())
			context.uiRenderer.fillColorUnaligned(
				minX, maxY, maxX, maxY, maxX - region.height / 2, minY, minX, minY, 0,
				Gradient(minX, minY, region.width, region.height, weakColor, 0, weakColor)
			)

			val textColor = srgbToLinear(rgb(238, 203, 127))
			context.uiRenderer.drawString(
				context.resources.font, enemy.monster.name, textColor, IntArray(0),
				nameX, region.minY, region.maxX, maxY, maxY - marginY,
				3 * region.height / 10, 1, TextAlignment.LEFT
			)
		}

		run {
			val healthBar = ResourceBarRenderer(context, ResourceType.Health, AbsoluteRectangle(
				region.minX + region.height / 2, region.minY + 6 * region.height / 10,
				78 * region.width / 100 - region.height / 2, 2 * region.height / 10
			))
			var displayedHealth = enemy.currentHealth
			val lastDamage = enemy.lastDamageIndicator
			if (lastDamage is DamageIndicatorHealth) {
				val passedTime = currentTime - lastDamage.time
				val duration = 2_000_000_000L
				val progress = passedTime.toDouble() / duration.toDouble()
				if (progress < 1.0) {
					val newHealth = lastDamage.oldHealth + lastDamage.gainedHealth
					displayedHealth = (progress * newHealth + (1.0 - progress) * lastDamage.oldHealth).roundToInt()
					displayedHealth = if (lastDamage.gainedHealth >= 0) min(displayedHealth, enemy.currentHealth)
					else max(displayedHealth, enemy.currentHealth)
				}
			}
			healthBar.renderBar(displayedHealth, enemy.maxHealth)
			healthBar.renderCurrentOverBar(displayedHealth, enemy.maxHealth)
		}

		run {
			val minX = region.minX + 80 * region.width / 100
			val color = srgbToLinear(rgb(239, 214, 95))
			context.uiRenderer.drawString(
				context.resources.font, "Lv${enemy.getLevel(context.updateContext)}", color, IntArray(0),
				minX, region.minY, region.maxX, region.maxY,
				region.maxY - region.height / 6, region.height / 4, 1, TextAlignment.LEFT
			)
		}

		run {
			val diameter = region.height - region.height / 10
			val minY = region.minY + region.height / 20
			val color = srgbToLinear(rgb(86, 63, 31))
			val margin = region.height / 10
			context.uiRenderer.fillCircle(
				region.minX + margin, minY + margin,
				region.minX + diameter - 1 - margin, minY + diameter - 1 - margin, color
			)
		}
		context.uiRenderer.endBatch()

		context.resources.kim2Renderer.submit(kimBatch, context.recorder, context.targetImage)

		maybeRenderSelectionBlink(enemy, context.uiRenderer, region)
	}
}
