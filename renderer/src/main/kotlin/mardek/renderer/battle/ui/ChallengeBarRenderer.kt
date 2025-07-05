package mardek.renderer.battle.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.skill.ReactionSkillType
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.state.ingame.battle.ReactionChallenge
import mardek.state.title.AbsoluteRectangle
import kotlin.math.min
import kotlin.math.roundToInt

class ChallengeBarRenderer(private val context: BattleRenderContext, private val region: AbsoluteRectangle) {

	private val challengeState = context.battle.getReactionChallenge()
	private lateinit var batch: KimBatch

	private var resultColor = 0
	private var opacity = 1f

	fun beforeRendering() {
		if (challengeState == null) return

		val currentTime = System.nanoTime()
		if (challengeState.clickedAfter == -1L) {
			val startFadeTime = challengeState.startTime + ReactionChallenge.DURATION
			if (currentTime > startFadeTime) {
				val stopFadeTime = startFadeTime + ReactionChallenge.FINAL_FADE_DURATION
				opacity = if (currentTime >= stopFadeTime) {
					0f
				} else {
					1f - (currentTime - startFadeTime).toFloat() / ReactionChallenge.FINAL_FADE_DURATION.toFloat()
				}
			}
		} else {
			val startResultFadeTime = challengeState.startTime + challengeState.clickedAfter
			val stopResultFadeTime = startResultFadeTime + ReactionChallenge.RESULT_FADE_DURATION
			if (currentTime > stopResultFadeTime) {
				val stopFadeTime = stopResultFadeTime + ReactionChallenge.FINAL_FADE_DURATION
				opacity = if (currentTime >= stopFadeTime) {
					0f
				} else {
					1f - (currentTime - stopResultFadeTime).toFloat() / ReactionChallenge.FINAL_FADE_DURATION.toFloat()
				}
			} else {
				val resultFade = 1f - (currentTime - startResultFadeTime).toFloat() / ReactionChallenge.RESULT_FADE_DURATION.toFloat()
				val resultAlpha = (200 * resultFade).roundToInt()
				resultColor = if (challengeState.wasPassed()) {
					rgba(0, 250, 0, resultAlpha)
				} else {
					rgba(250, 0, 0, resultAlpha)
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
		batch = if (icon.version == 1) context.resources.kim1Renderer.startBatch()
		else context.resources.kim2Renderer.startBatch()

		batch.requests.add(KimRequest(
			x = region.minX + region.height, y = region.minY + region.height / 10,
			scale = 0.8f * region.height.toFloat() / icon.height, sprite = icon, opacity = opacity
		))
	}

	fun render() {
		if (challengeState == null || opacity == 0f) return

		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context, 12)
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
			rectangles.fill(
				region.minX, region.minY, region.maxX, region.minY + borderHeight - 1,
				srgbToLinear(rgba(208, 193, 142, highAlpha)),
			)
			rectangles.fill(
				region.minX, region.maxY + 1 - borderHeight, region.maxX, region.maxY,
				srgbToLinear(rgba(208, 193, 142, highAlpha)),
			)
			rectangles.gradient(
				region.minX, region.minY + borderHeight, region.maxX, midY - 1,
				lightBottomColor, lightRightColor, lightTopColor
			)
			rectangles.gradient(
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
			rectangles.gradientWithBorder(
				minX, minY, maxX, maxY, borderWidth, borderWidth,
				borderColor, baseColor, rightColor, upColor
			)

			val innerMinX = (minX + width * ReactionChallenge.MIN_CLICK_AFTER / ReactionChallenge.DURATION).toInt()
			val innerMaxX = (minX + width * ReactionChallenge.MAX_CLICK_AFTER / ReactionChallenge.DURATION).toInt()
			val darkColor = srgbToLinear(rgba(210, 170, 70, highAlpha))
			val brightColor = srgbToLinear(rgba(250, 240, 140, highAlpha))
			rectangles.fill(innerMinX, minY, innerMaxX, minY + height / 4, darkColor)
			rectangles.fill(
				innerMinX, minY + height / 4,
				innerMaxX, minY + height / 4 + height / 3, brightColor
			)
			rectangles.fill(minX, minY, maxX, maxY, resultColor)
			rectangles.endBatch(context.recorder)

			val cursorMinY = region.minY - region.height / 3
			val cursorMaxY = region.maxY + region.height / 3

			val cursorX = if (challengeState!!.clickedAfter == -1L) {
				min(maxX, minX + (width * (System.nanoTime() - challengeState.startTime) / ReactionChallenge.DURATION).toInt())
			} else {
				min(maxX, minX + (width * challengeState.clickedAfter / ReactionChallenge.DURATION).toInt())
			}
			val cursorWidth = 3 * height / 2

			context.uiRenderer.beginBatch()
			context.uiRenderer.drawImage(
				context.resources.bcImages[context.content.ui.challengeCursor.index],
				cursorX - cursorWidth / 2, cursorMinY, cursorWidth, cursorMaxY - cursorMinY
			)
			context.uiRenderer.endBatch()
		}

		if (batch.requests[0].sprite.version == 1) {
			context.resources.kim1Renderer.submit(batch, context)
		} else {
			context.resources.kim2Renderer.submit(batch, context)
		}
	}
}
