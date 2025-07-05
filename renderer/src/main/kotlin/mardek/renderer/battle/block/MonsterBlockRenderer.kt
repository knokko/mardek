package mardek.renderer.battle.block

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.changeAlpha
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.roundToInt

class MonsterBlockRenderer(
	private val context: BattleRenderContext,
	private val enemy: MonsterCombatantState,
	private val region: AbsoluteRectangle,
) {
	private val currentTime = System.nanoTime()
	private val opacity: Float

	private var nameX = 0
	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch

	init {
		if (!enemy.isAlive()) {
			val lastDamage = enemy.lastDamageIndicator
			if (lastDamage != null) {
				val spentTime = currentTime - lastDamage.time
				val vanishTime = 1_500_000_000L
				opacity = 1f - spentTime.toFloat() / vanishTime.toFloat()
			} else opacity = 0f
		} else opacity = 1f
	}

	fun beforeRendering() {
		if (opacity <= 0f) return
		batch2 = context.resources.kim2Renderer.startBatch()
		val sprite = enemy.element.sprite
		val marginY = region.height / 20
		val desiredElementSize = region.height - 2 * marginY
		val scale = desiredElementSize / sprite.height.toFloat()
		batch2.requests.add(KimRequest(
			x = region.minX, y = region.minY + marginY, scale = scale, sprite = sprite
		))
		nameX = region.minX + (scale * sprite.width).roundToInt() + region.width / 50

		val numEffects = enemy.statusEffects.size
		if (numEffects > 0) {
			batch1 = context.resources.kim1Renderer.startBatch()
			val switchPeriod = 500_000_000L
			val relativeTime = (System.nanoTime() - context.battle.startTime) % (numEffects * switchPeriod)
			val index = (relativeTime / switchPeriod).toInt()
			val sprite = enemy.statusEffects.toList()[index].icon
			val desiredSize = 2 * region.height / 5
			val margin = (desiredElementSize - desiredSize) / 2
			batch1.requests.add(KimRequest(
				x = region.minX + margin, y = region.minY + marginY + margin,
				scale = desiredSize.toFloat() / sprite.height, sprite = sprite
			))
		}
	}

	fun render() {
		if (opacity <= 0f) return

		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context, 2)

		run {
			val mousePosition = context.battle.lastMousePosition
			if (mousePosition != null && region.contains(mousePosition.first, mousePosition.second)) {
				rectangles.fill(
					region.minX, region.minY, region.maxX, region.maxY,
					rgba(0, 200, 50, 10)
				)
			}
		}
		run {
			val marginY = region.height / 10
			val minX = region.minX + region.height / 2
			val minY = region.minY + marginY
			val maxX = minX + 3 * region.width / 4
			val maxY = region.minY + region.height / 2
			val weakColor = changeAlpha(enemy.element.color, 150)
			rectangles.gradientUnaligned(
				minX, maxY, weakColor,
				maxX, maxY, 0,
				maxX - region.height / 2, minY, 0,
				minX, minY, weakColor,
			)

			rectangles.endBatch(context.recorder)
			context.uiRenderer.beginBatch()

			val textColor = srgbToLinear(rgb(238, 203, 127))
			context.uiRenderer.drawString(
				context.resources.font, enemy.monster.name, textColor, IntArray(0),
				nameX, region.minY, region.maxX, maxY, maxY - marginY,
				3 * region.height / 10, 1, TextAlignment.LEFT
			)
		}

		val healthBar = ResourceBarRenderer(context, ResourceType.Health, AbsoluteRectangle(
			region.minX + region.height / 2, region.minY + 6 * region.height / 10,
			78 * region.width / 100 - region.height / 2, 2 * region.height / 10
		))
		val displayedHealth = renderCombatantHealth(enemy, healthBar, currentTime)
		healthBar.renderCurrentOverBar(displayedHealth, enemy.maxHealth)

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

		context.resources.kim2Renderer.submit(batch2, context)
		if (this::batch1.isInitialized) {
			context.resources.kim1Renderer.submit(batch1, context)
		}

		maybeRenderSelectionBlink(enemy, context.uiRenderer, region)
		enemy.renderedInfoBlock = region
	}
}
