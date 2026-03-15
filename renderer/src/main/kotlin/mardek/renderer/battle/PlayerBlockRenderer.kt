package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.state.ingame.battle.ExperienceIndicators
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.util.Rectangle
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun renderPlayerBlock(
	battleContext: BattleRenderContext, player: PlayerCombatantState,
	colorBatch: Vk2dColorBatch, lateColorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	spriteBatch: Vk2dKim3Batch, imageBatch: Vk2dImageBatch, textBatch: Vk2dGlyphBatch, region: Rectangle
) {
	battleContext.run {
		val nameX = run {
			val sprite = player.element.thickSprite
			val marginY = region.height * 0.025f
			val scale = (region.height - 2 * marginY) / sprite.height.toFloat()
			val y = region.minY + marginY
			imageBatch.coloredScale(
				region.minX.toFloat(), y, scale, sprite.index,
				0, rgba(1f, 1f, 1f, 0.7f),
			)
			region.minX + scale * sprite.width
		}

		run {
			val period = 1_000_000_000L
			val inPeriod = (System.nanoTime() - referenceTime) % period
			val walkingSprite = player.player.areaSprites.sprites[if (inPeriod < 500_000_000L) 0 else 1]
			val scale = 0.5f * region.height / walkingSprite.height
			spriteBatch.simple(
				region.minX + (6.5f * scale).roundToInt(),
				region.minY + (8f * scale).roundToInt(),
				scale, walkingSprite.index,
			)

			val numEffects = player.statusEffects.size
			if (numEffects > 0) {
				val switchPeriod = 500_000_000L
				val relativeTime = (System.nanoTime() - referenceTime) % (numEffects * switchPeriod)
				val index = (relativeTime / switchPeriod).toInt()
				val sprite = player.statusEffects.toList()[index].icon
				val desiredSize = region.height / 3
				imageBatch.simpleScale(
					region.maxX - desiredSize * 1.5f, region.minY.toFloat(),
					desiredSize.toFloat() / sprite.height, sprite.index,
				)
			}
		}

		val mousePosition = battle.lastMousePosition
		if (mousePosition != null && region.contains(mousePosition.first, mousePosition.second)) {
			colorBatch.fill(
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
			colorBatch.gradientUnaligned(
				minX, maxY, weakColor,
				maxX, maxY, 0,
				maxX - region.height / 2, minY, 0,
				minX, minY, weakColor,
			)

			val font = context.bundle.getFont(context.content.fonts.fat.index)
			val textColor = srgbToLinear(rgb(238, 203, 127))
			val strokeColor = srgbToLinear(rgb(31, 27, 22))
			val shadowColor = srgbToLinear(rgb(77, 64, 53))
			val shadowOffset = 0.02f * region.height
			textBatch.drawShadowedString(
				player.player.name, nameX, maxY - marginY * 0.5f,
				0.18f * region.height, font, textColor, strokeColor,
				0.015f * region.height, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
		}

		run {
			val healthBar = ResourceBarRenderer(
				context, ResourceType.Health, Rectangle(
					region.minX + 5 * region.height / 6, region.minY + 13 * region.height / 30,
					region.width - 5 * region.height / 6 - region.width / 20, 2 * region.height / 12
				), colorBatch, textBatch,
			)
			val displayedHealth = renderCombatantHealth(player, healthBar, System.nanoTime())
			healthBar.renderTextOverBar(displayedHealth, player.maxHealth)
			healthBar.renderClosingBracket()

			val xpBar = ResourceBarRenderer(
				context, ResourceType.Experience, Rectangle(
					region.minX + 2 * region.height / 3, region.maxY - 4 * region.height / 13,
					region.width / 3, region.height / 6
				), colorBatch, textBatch,
			)
			val playerState = state.characterStates[player.player]!!
			xpBar.renderBar(playerState.experienceToNextLevel, playerState.experienceForNextLevel())
			xpBar.renderClosingBracket()

			val font = context.bundle.getFont(context.content.fonts.large1.index)
			val shadowColor = rgba(0, 0, 0, 200)
			val shadowOffset = 0.015f * region.height
			textBatch.drawShadowedString(
				"Lv${player.getLevel(updateContext)}", nameX, region.maxY - region.height * 0.12f,
				0.2f * region.height, font,
				srgbToLinear(rgb(251, 225, 100)), 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)

			val manaBar = ResourceBarRenderer(
				context, ResourceType.Mana, Rectangle(
					region.maxX - region.width / 3 - region.width / 20, region.maxY - 4 * region.height / 13,
					region.width / 3, region.height / 6
				), colorBatch, textBatch,
			)
			manaBar.renderBar(player.currentMana, player.maxMana)
			manaBar.renderCurrentOverBar(player.currentMana, player.maxMana)
			manaBar.renderOpeningBracket()
			manaBar.renderClosingBracket()

			val recentExp = player.experienceIndicators.getEntryToDisplay(renderTime)
			if (recentExp != null) {
				var passedTime = renderTime - recentExp.startTime
				var offsetY = 0f
				var alpha = 255
				if (passedTime < ExperienceIndicators.JUMP_DURATION) {
					val relativeTime = passedTime.toFloat() / ExperienceIndicators.JUMP_DURATION
					val fromMidTime = 0.5f - relativeTime
					offsetY = (0.25f - fromMidTime.pow(2)) * 0.5f * region.height
				}
				passedTime -= ExperienceIndicators.JUMP_DURATION + ExperienceIndicators.STABLE_DURATION
				if (passedTime > 0) {
					alpha = (255f * (1f - passedTime.toFloat() / ExperienceIndicators.FADE_DURATION)).roundToInt()
				}

				if (alpha > 0) {
					val fatFont = context.bundle.getFont(context.content.fonts.basic1.index)
					textBatch.drawString(
						"+${recentExp.amount} EXP", region.minX + 0.2f * region.height,
						region.minY + 0.2f * region.height - offsetY, 0.2f * region.height, fatFont,
						srgbToLinear(rgba(254, 201, 9, alpha)),
						srgbToLinear(rgba(38, 16, 9, alpha)),
						0.025f * region.height, TextAlignment.LEFT,
					)
				}
			}
		}

		run {
			val diameter = region.height - region.height / 20
			val minY = region.minY + region.height / 40
			val color = srgbToLinear(rgb(86, 63, 31))
			val margin = region.height / 10
			ovalBatch.simpleAntiAliased(
				region.minX + margin, minY + margin,
				region.minX + diameter - 1 - margin, minY + diameter - 1 - margin,
				0.2f, color
			)
		}

		maybeRenderSelectionBlink(player, lateColorBatch, region)

		player.renderInfo.renderedInfoBlock = region
	}
}
