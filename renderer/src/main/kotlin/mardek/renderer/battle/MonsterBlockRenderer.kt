package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderMonsterBlock(
	battleContext: BattleRenderContext, enemy: MonsterCombatantState,
	colorBatch: Vk2dColorBatch, lateColorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	imageBatch: Vk2dImageBatch, textBatch: Vk2dGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val opacity = if (!enemy.isAlive()) {
			val lastDamage = enemy.renderInfo.lastDamageIndicator
			if (lastDamage != null) {
				val spentTime = renderTime - lastDamage.time
				val vanishTime = 1_500_000_000L
				1f - spentTime.toFloat() / vanishTime.toFloat()
			} else return
		} else 1f
		if (opacity <= 0f) return

		val sprite = enemy.element.thickSprite
		val marginY = region.height / 40
		val desiredElementSize = region.height - 2 * marginY
		val scale = desiredElementSize / sprite.height.toFloat()
		imageBatch.coloredScale(
			region.minX.toFloat(), region.minY + marginY.toFloat(),
			scale, sprite.index, 0,
			rgba(1f, 1f, 1f, opacity)
		)
		val nameX = region.minX + (scale * sprite.width).roundToInt() + region.width / 50

		val numEffects = enemy.statusEffects.size
		if (numEffects > 0) {
			val switchPeriod = 500_000_000L
			val relativeTime = (renderTime - battle.startTime) % (numEffects * switchPeriod)
			val index = (relativeTime / switchPeriod).toInt()
			val sprite = enemy.statusEffects.toList()[index].icon
			val desiredSize = 2 * region.height / 5
			val margin = (desiredElementSize - desiredSize) * 0.5f
			imageBatch.coloredScale(
				region.minX + margin, region.minY + marginY + margin,
				desiredSize.toFloat() / sprite.height, sprite.index,
				0, rgba(1f, 1f, 1f, opacity),
			)
		}

		val mousePosition = battle.lastMousePosition
		if (mousePosition != null && region.contains(mousePosition.first, mousePosition.second)) {
			colorBatch.fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 200, 50, (10 * opacity).roundToInt())
			)
		}

		run {
			val marginY = region.height / 10
			val minX = region.minX + region.height / 2
			val minY = region.minY + marginY
			val maxX = minX + 3 * region.width / 4
			val maxY = region.minY + region.height / 2
			val weakColor = changeAlpha(enemy.element.color, 0.6f * opacity)
			colorBatch.gradientUnaligned(
				minX, maxY, weakColor,
				maxX, maxY, 0,
				maxX - region.height / 2, minY, 0,
				minX, minY, weakColor,
			)

			val font = context.bundle.getFont(context.content.fonts.fat.index)
			val textColor = srgbToLinear(rgb(238, 203, 127))
			textBatch.drawString(
				enemy.monster.displayName, nameX.toFloat(), maxY.toFloat() - marginY,
				0.3f * region.height, font, changeAlpha(textColor, opacity),
			)
		}

		val healthBar = ResourceBarRenderer(
			context, ResourceType.Health, Rectangle(
				region.minX + region.height * 5 / 9, region.minY + 6 * region.height / 10,
				78 * region.width / 100 - region.height * 5 / 9, 2 * region.height / 10
			), colorBatch, textBatch
		)
		val displayedHealth = renderCombatantHealth(enemy, healthBar, renderTime, opacity)
		healthBar.renderCurrentOverBar(displayedHealth, enemy.maxHealth, opacity)

		run {
			val minX = region.minX + 80 * region.width / 100
			val color = srgbToLinear(rgb(239, 214, 95))
			val font = context.bundle.getFont(context.content.fonts.large1.index)
			val shadowColor = rgba(0, 0, 0, 200)
			val shadowOffset = 0.015f * region.height
			textBatch.drawShadowedString(
				"Lv${enemy.getLevel(updateContext)}",
				minX.toFloat(), region.maxY - 0.16f * region.height,
				0.25f * region.height, font, changeAlpha(color, opacity),
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
		}

		run {
			val diameter = region.height - region.height / 20
			val minY = region.minY + region.height / 40
			val color = srgbToLinear(rgb(86, 63, 31))
			val margin = region.height / 10
			ovalBatch.simpleAntiAliased(
				region.minX + margin, minY + margin,
				region.minX + diameter - 1 - margin, minY + diameter - 1 - margin,
				0.2f, changeAlpha(color, opacity),
			)
		}

		maybeRenderSelectionBlink(enemy, lateColorBatch, region)
		enemy.renderInfo.renderedInfoBlock = region
	}
}
