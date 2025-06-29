package mardek.renderer.battle.block

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.changeAlpha
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.title.AbsoluteRectangle
import kotlin.math.roundToInt

class PlayerBlockRenderer(
	private val context: BattleRenderContext,
	private val player: PlayerCombatantState,
	private val region: AbsoluteRectangle,
) {

	private var nameX = 0
	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch

	fun beforeRendering() {
		run {
			batch2 = context.resources.kim2Renderer.startBatch()
			val sprite = player.element.sprite // TODO thin sprite?
			val marginY = region.height / 20
			val scale = (region.height - 2 * marginY) / sprite.height.toFloat()
			batch2.requests.add(KimRequest(
				x = region.minX, y = region.minY + marginY, scale = scale, sprite = sprite
			))
			nameX = region.minX + (scale * sprite.width).roundToInt()
		}

		run {
			batch1 = context.resources.kim1Renderer.startBatch()
			val period = 1_000_000_000L
			val inPeriod = (System.nanoTime() - context.battle.startTime) % period
			val walkingSprite = player.player.areaSprites.sprites[if (inPeriod < 500_000_000L) 0 else 1]
			val scale = 0.5f * region.height / walkingSprite.height
			batch1.requests.add(KimRequest(
				x = region.minX + (6.5f * scale).roundToInt(),
				y = region.minY + (8f * scale).roundToInt(),
				scale = scale, sprite = walkingSprite
			))

			val numEffects = player.statusEffects.size
			if (numEffects > 0) {
				val switchPeriod = 500_000_000L
				val relativeTime = (System.nanoTime() - context.battle.startTime) % (numEffects * switchPeriod)
				val index = (relativeTime / switchPeriod).toInt()
				val sprite = player.statusEffects.toList()[index].icon
				val desiredSize = region.height / 3
				batch1.requests.add(KimRequest(
					x = region.maxX - desiredSize - desiredSize / 2, y = region.minY,
					scale = desiredSize.toFloat() / sprite.height, sprite = sprite
				))
			}
		}
	}

	fun render() {
		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context.recorder, context.targetImage, 2)

		val mousePosition = context.battle.lastMousePosition
		if (mousePosition != null && region.contains(mousePosition.first, mousePosition.second)) {
			rectangles.fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 200, 50, 10)
			)
		}

		run {
			val element = player.element
			val marginY = region.height / 10
			val minX = region.minX + region.height / 2
			val minY = region.minY + marginY
			val maxX = minX + 3 * region.width / 4
			val maxY = region.minY + region.height / 3
			val weakColor = changeAlpha(element.color, 150)
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
				context.resources.font, player.player.name, textColor, IntArray(0),
				nameX, region.minY, region.maxX, maxY, maxY - marginY / 2,
				3 * region.height / 15, 1, TextAlignment.LEFT
			)
		}

		run {
			val healthBar = ResourceBarRenderer(context, ResourceType.Health, AbsoluteRectangle(
				region.minX + 4 * region.height / 5, region.minY + 13 * region.height / 30,
				region.width - 4 * region.height / 5 - region.width / 20, 2 * region.height / 12
			))
			val displayedHealth = renderCombatantHealth(player, healthBar, System.nanoTime())
			healthBar.renderTextOverBar(displayedHealth, player.maxHealth)

			val xpBar = ResourceBarRenderer(
				context, ResourceType.Experience, AbsoluteRectangle(
					region.minX + 2 * region.height / 3, region.maxY - 4 * region.height / 13,
					region.width / 3, region.height / 6
				)
			)
			xpBar.renderBar(80, 100) // TODO proper xp
			context.uiRenderer.drawString(
				context.resources.font, "Lv${player.getLevel(context.updateContext)}",
				srgbToLinear(rgb(251, 225, 100)), IntArray(0),
				nameX, region.minY, region.maxX, region.maxY, region.maxY - region.height / 8,
				region.height / 5, 1, TextAlignment.LEFT
			)

			val manaBar = ResourceBarRenderer(
				context, ResourceType.Mana, AbsoluteRectangle(
					region.maxX - region.width / 3 - region.width / 20, region.maxY - 4 * region.height / 13,
					region.width / 3, region.height / 6
				)
			)
			manaBar.renderBar(player.currentMana, player.maxMana)
			manaBar.renderCurrentOverBar(player.currentMana, player.maxMana)
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

		context.resources.kim2Renderer.submit(batch2, context.recorder, context.targetImage)
		context.resources.kim1Renderer.submit(batch1, context.recorder, context.targetImage)

		maybeRenderSelectionBlink(player, context.uiRenderer, region)

		player.renderedInfoBlock = region
	}
}
