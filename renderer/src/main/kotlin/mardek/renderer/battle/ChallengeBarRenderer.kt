package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import mardek.content.skill.ReactionSkillType
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.battle.ReactionChallenge
import mardek.state.util.Rectangle
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderChallengeBar(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch,
	imageBatch: Vk2dImageBatch, region: Rectangle,
) {
	battleContext.run {
		val challengeState = battle.getReactionChallenge() ?: return

		val (opacity, resultColor) = if (challengeState.clickedAfter == -1L) {
			val startFadeTime = challengeState.startTime + ReactionChallenge.DURATION
			if (renderTime > startFadeTime) {
				val stopFadeTime = startFadeTime + ReactionChallenge.FINAL_FADE_DURATION
				if (renderTime >= stopFadeTime) {
					Pair(0f, 0)
				} else {
					Pair(1f - (renderTime - startFadeTime).toFloat() / ReactionChallenge.FINAL_FADE_DURATION.toFloat(), 0)
				}
			} else Pair(1f, 0)
		} else {
			val startResultFadeTime = challengeState.startTime + challengeState.clickedAfter
			val stopResultFadeTime = startResultFadeTime + ReactionChallenge.RESULT_FADE_DURATION
			if (renderTime > stopResultFadeTime) {
				val stopFadeTime = stopResultFadeTime + ReactionChallenge.FINAL_FADE_DURATION
				if (renderTime >= stopFadeTime) {
					Pair(0f, 0)
				} else {
					Pair(1f - (renderTime - stopResultFadeTime).toFloat() / ReactionChallenge.FINAL_FADE_DURATION.toFloat(), 0)
				}
			} else {
				val resultFade = 1f - (renderTime - startResultFadeTime).toFloat() / ReactionChallenge.RESULT_FADE_DURATION.toFloat()
				val resultAlpha = (200 * resultFade).roundToInt()
				if (challengeState.wasPassed()) {
					Pair(1f, rgba(0, 250, 0, resultAlpha))
				} else {
					Pair(1f, rgba(250, 0, 0, resultAlpha))
				}
			}
		}

		if (opacity == 0f) return

		val icon = when (challengeState.primaryType) {
			ReactionSkillType.MeleeAttack -> context.content.ui.meleeAttackIcon
			ReactionSkillType.MeleeDefense -> context.content.ui.meleeDefenseIcon
			ReactionSkillType.RangedAttack -> context.content.ui.rangedAttackIcon
			ReactionSkillType.RangedDefense -> context.content.ui.rangedDefenseIcon
		}

		imageBatch.coloredScale(
			region.minX + region.height.toFloat(), region.minY + 0.1f * region.height,
			0.8f * region.height.toFloat() / icon.height, icon.index,
			0, rgba(1f, 1f, 1f, opacity),
		)

		val highAlpha = (255 * opacity).roundToInt()
		run {
			val alpha = (220 * opacity).roundToInt()
			val lightBottomColor = srgbToLinear(rgba(80, 65, 55, alpha))
			val lightTopColor = srgbToLinear(rgba(120, 110, 110, alpha))
			val lightRightColor = srgbToLinear(rgba(130, 110, 70, alpha))
			val darkLeftColor = srgbToLinear(rgba(38, 32, 32, alpha))
			val darkRightColor = srgbToLinear(rgba(100, 90, 50, alpha))
			val midY = region.minY + region.height / 2
			val borderHeight = region.height / 25
			colorBatch.fill(
				region.minX, region.minY, region.maxX, region.minY + borderHeight - 1,
				srgbToLinear(rgba(208, 193, 142, highAlpha)),
			)
			colorBatch.fill(
				region.minX, region.maxY + 1 - borderHeight, region.maxX, region.maxY,
				srgbToLinear(rgba(208, 193, 142, highAlpha)),
			)
			colorBatch.gradient(
				region.minX, region.minY + borderHeight, region.maxX, midY - 1,
				lightBottomColor, lightRightColor, lightTopColor
			)
			colorBatch.gradient(
				region.minX, midY, region.maxX, region.maxY - borderHeight,
				darkLeftColor, darkRightColor, darkLeftColor
			)
		}
		run {
			val baseColor = srgbToLinear(rgba(73, 56, 29, highAlpha))
			val rightColor = srgbToLinear(rgba(39, 27, 18, highAlpha))
			val upColor = srgbToLinear(rgba(82, 64, 36, highAlpha))
			val borderColor = srgbToLinear(rgba(209, 158, 54, highAlpha))
			val borderWidth = region.height / 25
			val minX = region.minX + 2 * region.height
			val minY = region.minY + region.height / 3
			val maxX = region.maxX - 2 * region.height
			val maxY = region.maxY - region.height / 3
			val width = 1 + maxX - minX
			val height = 1 + maxY - minY
			gradientWithBorder(
				colorBatch, minX, minY, maxX, maxY, borderWidth, borderWidth,
				borderColor, baseColor, rightColor, upColor
			)

			val innerMinX = (minX + width * ReactionChallenge.MIN_CLICK_AFTER / ReactionChallenge.DURATION).toInt()
			val innerMaxX = (minX + width * ReactionChallenge.MAX_CLICK_AFTER / ReactionChallenge.DURATION).toInt()
			val darkColor = srgbToLinear(rgba(210, 170, 70, highAlpha))
			val brightColor = srgbToLinear(rgba(250, 240, 140, highAlpha))
			colorBatch.fill(innerMinX, minY, innerMaxX, minY + height / 4, darkColor)
			colorBatch.fill(
				innerMinX, minY + height / 4,
				innerMaxX, minY + height / 4 + height / 3, brightColor
			)
			colorBatch.fill(minX, minY, maxX, maxY, resultColor)

			val cursorMinY = region.minY - region.height / 3
			val cursorMaxY = region.maxY + region.height / 3

			val cursorX = if (challengeState.clickedAfter == -1L) {
				min(maxX, minX + (width * (System.nanoTime() - challengeState.startTime) / ReactionChallenge.DURATION).toInt())
			} else {
				min(maxX, minX + (width * challengeState.clickedAfter / ReactionChallenge.DURATION).toInt())
			}
			val cursorWidth = 3 * height / 2

			imageBatch.colored(
				cursorX - 0.5f * cursorWidth, cursorMinY.toFloat(),
				cursorX + 0.5f * cursorWidth, cursorMaxY.toFloat(),
				context.content.ui.challengeCursor.index,
				0, rgba(1f, 1f, 1f, opacity),
			)
		}
	}
}
