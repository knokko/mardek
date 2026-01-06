package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.*
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.renderPortraitAnimation
import mardek.renderer.menu.referenceTime
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal fun renderDialogue(areaContext: AreaRenderContext) {
	areaContext.run {
		val suspension = state.suspension
		val actions = if (suspension is AreaSuspensionActions) suspension.actions else return
		val actionNode = actions.node ?: return

		var talkAction: ActionTalk? = null

		if (actionNode is FixedActionNode) {
			val action = actionNode.action
			if (action is ActionTalk) talkAction = action
			if (action is ActionParallel) {
				for (parallelAction in action.actions) {
					if (parallelAction is ActionTalk) talkAction = parallelAction
				}
			}
		}

		val choiceChar = '•'
		if (actionNode is ChoiceActionNode) {
			val combinedText = actionNode.options.withIndex().joinToString("\n") {
				var text = "$choiceChar " + it.value.text
				if (it.index == actions.selectedChoice) text = "$$text%"
				text
			}
			talkAction = ActionTalk(actionNode.speaker, actionNode.expression, combinedText)
		}

		if (talkAction == null) return

		val portrait = when (val speaker = talkAction.speaker) {
			is ActionTargetPlayer -> speaker.player.portraitInfo
			is ActionTargetPartyMember -> context.campaign.party[speaker.index]?.portraitInfo
			is ActionTargetAreaCharacter -> speaker.character.portrait
			else -> null
		}

		val textRegionHeight = region.height / 4
		val textRegion = Rectangle(
			region.minX, region.boundY - textRegionHeight,
			region.width, textRegionHeight,
		)

		if (portrait != null) {
			val magicScale = region.height / 250f

			val renderWidth = 60f * magicScale
			val renderHeight = 62f * magicScale

			val renderX = textRegion.minX + renderWidth
			val renderY = textRegion.minY - renderHeight

			val animationContext = AnimationContext(
				renderRegion = Rectangle(region.minX, region.minY, region.width, region.height - textRegionHeight),
				renderTime = System.nanoTime(),
				magicScale = context.content.portraits.magicScale,
				parentMatrix = Matrix3x2f().translate(renderX, renderY).scale(-magicScale, magicScale),
				parentColorTransform = null,
				partBatch = portraitBatch,
				noMask = context.content.battle.noMask,
				combat = null,
				portrait = portrait,
				portraitExpression = talkAction.expression,
			)
			renderPortraitAnimation(context.content.portraits.animations, animationContext)
		}

		run {
			val highColor = srgbToLinear(rgba(109, 91, 52, 242))
			val lowColor = srgbToLinear(rgba(24, 14, 10, 230))
			colorBatch.gradient(
				textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY,
				lowColor, lowColor, highColor,
			)
		}

		run {
			val lineColor = srgbToLinear(rgb(208, 193, 142))
			val offset = region.height / 200
			val lineWidth = max(1, region.height / 500)

			val minX = textRegion.minX + offset
			val minY = textRegion.minY + offset
			val maxX = textRegion.maxX - offset
			val maxY = textRegion.maxY - offset
			colorBatch.fill(minX, minY, maxX, minY + lineWidth - 1, lineColor)
			colorBatch.fill(minX, minY, minX + lineWidth - 1, maxY, lineColor)
			colorBatch.fill(minX, maxY + 1 - lineWidth, maxX, maxY, lineColor)
			colorBatch.fill(maxX + 1 - lineWidth, minY, maxX, maxY, lineColor)
		}

		val nameRegionHeight = region.height / 12
		val nameRegion = Rectangle(
			region.minX, textRegion.minY - nameRegionHeight,
			region.width, nameRegionHeight,
		)

		run {
			val backgroundColor = srgbToLinear(rgba(24, 14, 10, 230))
			val borderColor = srgbToLinear(rgb(73, 52, 37))
			val lineWidth = max(1, region.height / 400)
			val diagonalX1 = nameRegion.maxX - nameRegion.height * 3
			val diagonalHeight = region.height / 25
			val diagonalX2 = diagonalX1 + diagonalHeight
			portraitBackgroundBatch.fill(
				nameRegion.minX, nameRegion.minY, diagonalX1 - 1,
				nameRegion.minY + lineWidth - 1, borderColor,
			)
			portraitBackgroundBatch.fill(
				nameRegion.minX, nameRegion.boundY - lineWidth,
				nameRegion.maxX, nameRegion.maxY, borderColor,
			)
			portraitBackgroundBatch.fill(
				nameRegion.minX, nameRegion.minY + lineWidth,
				nameRegion.maxX, nameRegion.maxY - lineWidth, backgroundColor,
			)
			portraitBackgroundBatch.fill(
				diagonalX2, nameRegion.minY - diagonalHeight,
				nameRegion.maxX, nameRegion.minY + lineWidth - 1 - diagonalHeight, borderColor,
			)
			portraitBackgroundBatch.fillUnaligned(
				diagonalX1, nameRegion.minY + lineWidth,
				diagonalX2, nameRegion.minY + lineWidth + 1 - diagonalHeight,
				diagonalX2, nameRegion.minY - diagonalHeight,
				diagonalX1, nameRegion.minY, borderColor,
			)
			portraitBackgroundBatch.fillUnaligned(
				diagonalX1 + 1, nameRegion.minY + lineWidth,
				nameRegion.boundX, nameRegion.minY + lineWidth,
				nameRegion.boundX, nameRegion.minY + lineWidth - diagonalHeight,
				diagonalX2 + 1, nameRegion.minY + lineWidth - diagonalHeight, backgroundColor
			)
		}

		fun renderBox(
			x: Int, boxY: Int, boxSize: Int, borderWidth: Int, boxRadius: Int, cornerDistances: FloatArray,
			boxColor: Int, token: String, label: String,
		) {
			val tokenFont = context.bundle.getFont(context.content.fonts.basic2.index)
			val labelFont = context.bundle.getFont(context.content.fonts.large1.index)
			val textColor = srgbToLinear(rgb(186, 146, 77))
			val shadowColor = rgb(0, 0, 0)
			val shadowOffset = boxSize * 0.08f

			colorBatch.fill(
				x + boxRadius, boxY + borderWidth,
				x + boxSize - 1 - boxRadius, boxY + boxSize - 1 - borderWidth,
				boxColor,
			)
			colorBatch.fill(
				x + borderWidth, boxY + boxRadius,
				x + boxRadius - 1, boxY + boxSize - 1 - boxRadius,
				boxColor
			)
			colorBatch.fill(
				x + boxSize - boxRadius, boxY + boxRadius,
				x + boxSize - 1 - borderWidth, boxY + boxSize - 1 - boxRadius,
				boxColor
			)

			val borderColor = srgbToLinear(rgba(73, 52, 37, 150))
			colorBatch.fill(
				x, boxY + boxRadius,
				x + borderWidth - 1, boxY + boxSize - boxRadius - 1,
				borderColor,
			)
			colorBatch.fill(
				x + boxSize - borderWidth, boxY + boxRadius,
				x + boxSize - 1, boxY + boxSize - boxRadius - 1,
				borderColor,
			)
			colorBatch.fill(
				x + boxRadius, boxY,
				x + boxSize - boxRadius - 1, boxY + borderWidth - 1,
				borderColor,
			)
			colorBatch.fill(
				x + boxRadius, boxY + boxSize - borderWidth,
				x + boxSize - boxRadius - 1, boxY + boxSize - 1,
				borderColor,
			)

			val r = boxRadius.toFloat()

			fun renderQuarterOval(minX: Int, minY: Int, maxX: Int, maxY: Int, centerX: Float, centerY: Float) {
				ovalBatch.complex(
					minX, minY, maxX, maxY, centerX, centerY, r, r,
					boxColor, boxColor, borderColor, borderColor, 0,
					cornerDistances[0], cornerDistances[1],
					cornerDistances[2], cornerDistances[3],
				)
			}

			renderQuarterOval(
				x, boxY,x + boxRadius - 1, boxY + boxRadius - 1,
				x + r, boxY + r,
			)
			renderQuarterOval(
				x, boxY + boxSize - boxRadius,x + boxRadius - 1, boxY + boxSize - 1,
				x + r, boxY + boxSize - r,
			)
			renderQuarterOval(
				x + boxSize - boxRadius, boxY,x + boxSize - 1, boxY + boxRadius - 1,
				x + boxSize - r, boxY + r,
			)
			renderQuarterOval(
				x + boxSize - boxRadius - 1, boxY + boxSize - boxRadius,
				x + boxSize - 1, boxY + boxSize - 1,
				x + boxSize - r, boxY + boxSize - r,
			)

			val tokenBaseX = x + boxSize * 0.45f
			val textY = boxY + boxSize * 0.7f
			val textHeight = boxSize * 0.5f
			if (label.isEmpty()) {
				val highColor = srgbToLinear(rgb(109, 93, 81))
				textBatch.drawFancyString(
					token, tokenBaseX, textY + 0.1f * boxSize, textHeight, tokenFont,
					borderColor, 0, 0f, TextAlignment.CENTERED,
					borderColor, highColor, highColor, highColor,
					0.5f, 0.5f, 1f, 1f,
				)
			} else {
				textBatch.drawShadowedString(
					token, tokenBaseX, textY, textHeight,
					tokenFont, textColor, 0, 0f, shadowColor,
					shadowOffset, shadowOffset, TextAlignment.CENTERED,
				)
				textBatch.drawShadowedString(
					label, x + boxSize * 1.3f, textY, textHeight,
					labelFont, textColor, 0, 0f, shadowColor,
					shadowOffset, shadowOffset, TextAlignment.LEFT,
				)
			}
		}

		run {
			val boxY = nameRegion.minY - nameRegion.height * 3 / 8
			val boxSize = nameRegion.height * 3 / 8
			val boxRadius = nameRegion.height * 3 / 28
			val borderWidth = max(1, boxSize / 30)
			val boxColor = srgbToLinear(rgb(88, 71, 47))
			val cornerDistances = floatArrayOf(0.85f, 0.9f, 1.0f, 1.05f)

			renderBox(
				nameRegion.maxX - nameRegion.height * 10 / 4, boxY,
				boxSize, borderWidth, boxRadius, cornerDistances, boxColor, "Q", "Skip",
			)
			// TODO CHAP2 Make the chat log...
			renderBox(
				nameRegion.maxX - nameRegion.height * 5 / 4, boxY,
				boxSize, borderWidth, boxRadius, cornerDistances, boxColor, "L", "Log",
			)
		}

		if (actions.shownDialogueCharacters >= talkAction.text.length || actionNode is ChoiceActionNode) {
			val boxSizePeriod = 1_000_000_000L
			val relativeTime = ((System.nanoTime() - referenceTime) % boxSizePeriod).toFloat() / boxSizePeriod
			val minBoxSize = textRegion.height * 0.24f
			val maxBoxSize = textRegion.height * 0.26f
			val floatBoxSize = minBoxSize + (2f * abs(0.5f - relativeTime)) * (maxBoxSize - minBoxSize)
			val boxSize = floatBoxSize.roundToInt()
			val cornerRadius = (minBoxSize / 6f).roundToInt()
			val darkColor = srgbToLinear(rgb(145, 137, 112))
			val lightColor = srgbToLinear(rgb(167, 161, 141))
			val cornerDistances = floatArrayOf(0.6f, 0.65f, 1f, 1.05f)
			val borderWidth = max(1, boxSize / 15)

			val boxOffset = (textRegion.height * 0.03f + minBoxSize + 0.5f * (boxSize - minBoxSize)).roundToInt()
			val boxX = textRegion.maxX - boxOffset
			val boxY = textRegion.maxY - boxOffset
			renderBox(
				boxX, boxY, boxSize, borderWidth, cornerRadius, cornerDistances,
				darkColor, "E", "",
			)
			colorBatch.fill(
				boxX + 5 * borderWidth / 2, boxY + 4 * borderWidth,
				boxX + boxSize - 1 - 5 * borderWidth / 2, boxY + 5 * boxSize / 9, lightColor
			)
			colorBatch.fill(
				boxX + 4 * borderWidth, boxY + 5 * borderWidth / 2,
				boxX + boxSize - 1 - 4 * borderWidth, boxY + 4 * borderWidth - 1, lightColor
			)

			val radius = borderWidth * 1.5f
			ovalBatch.aliased(
				boxX + 5 * borderWidth / 2, boxY + 5 * borderWidth / 2,
				boxX + 4 * borderWidth - 1, boxY + 4 * borderWidth - 1,
				boxX + 4f * borderWidth, boxY + 4f * borderWidth,
				radius, radius, lightColor,
			)
			ovalBatch.aliased(
				boxX + boxSize - 4 * borderWidth - 1, boxY + 5 * borderWidth / 2,
				boxX + boxSize - 5 * borderWidth / 2 - 1, boxY + 4 * borderWidth - 1,
				boxX + boxSize - 4f * borderWidth, boxY + 4f * borderWidth,
				radius, radius, lightColor,
			)
		}

		run {
			val speakerElement = when (val speaker = talkAction.speaker) {
				is ActionTargetPlayer -> speaker.player.element
				is ActionTargetPartyMember -> context.campaign.party[speaker.index]?.element
				is ActionTargetAreaCharacter -> speaker.character.element
				else -> null
			}

			if (speakerElement != null) {
				val image = speakerElement.thinSprite
				val desiredHeight = 0.8f * textRegion.height
				val scale = desiredHeight / image.height

				// TODO CHAP2 Find a less ugly way to handle this
				val alpha = if (speakerElement.rawName == "DARK") 0.4f else 0.15f

				imageBatch.coloredScale(
					textRegion.maxX - 1.2f * desiredHeight,
					textRegion.maxY - 0.9f * textRegion.height,
					scale, image.index, 0,
					rgba(1f, 1f, 1f, alpha),
				)
			}
		}

		run {
			val displayName = when (val speaker = talkAction.speaker) {
				is ActionTargetPlayer -> speaker.player.name
				is ActionTargetPartyMember -> context.campaign.party[speaker.index]?.name
				is ActionTargetDialogueObject -> speaker.displayName
				is ActionTargetAreaCharacter -> speaker.character.name
				else -> null
			}
			if (displayName != null) {
				val shadowOffset = nameRegion.height * 0.04f
				val textColor = srgbToLinear(rgb(238, 203, 127))
				val shadowColor = srgbToLinear(rgb(90, 65, 38))
				val font = context.bundle.getFont(context.content.fonts.basic2.index)
				val baseX = nameRegion.minX + region.height * if (portrait == null) 0.05f else 0.325f
				textBatch.drawShadowedString(
					displayName, baseX, nameRegion.maxY - nameRegion.height * 0.3f,
					nameRegion.height * 0.45f, font, textColor, 0, 0f,
					shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
				)
			}
		}

		run {
			val baseFont = context.bundle.getFont(context.content.fonts.fat.index)
			val choiceCharFont = context.bundle.getFont(context.content.fonts.basic2.index)
			var baseTextColor = srgbToLinear(rgb(207, 192, 141))
			var boldTextColor = srgbToLinear(rgb(253, 218, 116))
			val strokeColor = srgbToLinear(rgb(41, 34, 20))

			if (actionNode is ChoiceActionNode) {
				baseTextColor = srgbToLinear(rgb(68, 68, 68))
				boldTextColor = srgbToLinear(rgb(164, 204, 253))
			}

			val text = talkAction.text

			val baseTextX = textRegion.minX + textRegion.height * 0.1f
			val maxTextX = textRegion.maxX - textRegion.height * 0.1f
			var textX = baseTextX
			var textY = textRegion.minY + textRegion.height * 0.2f

			val textHeight = textRegion.height * 0.09f
			val strokeWidth = textRegion.height * 0.01f

			var remaining = actions.shownDialogueCharacters
			if (actionNode is ChoiceActionNode) remaining = talkAction.text.length.toFloat()
			val textChars = text.chars().toArray()

			var isBold = false
			for (charIndex in 0 until textChars.size) {
				if (remaining <= 0f) break
				val nextChar = textChars[charIndex]
				val font = if (nextChar == choiceChar.code) choiceCharFont else baseFont

				var peekX = textX
				for (peekIndex in charIndex until textChars.size) {
					val peekChar = textChars[peekIndex]
					if (peekChar == ' '.code || peekChar == '\n'.code) break
					if (peekChar == '%'.code || peekChar == '$'.code) continue

					val peekGlyph = font.getGlyphForChar(peekChar)
					peekX += textHeight * font.getGlyphAdvance(peekGlyph)
					if (peekX > maxTextX) {
						textX = baseTextX
						textY += 0.14f * textRegion.height
						break
					}
				}

				if (nextChar == '\n'.code) {
					textX = baseTextX
					textY += 0.17f * textRegion.height
					continue
				}

				if (nextChar == '$'.code) {
					isBold = true
					continue
				}

				if (nextChar == '%'.code) {
					isBold = false
					continue
				}

				val textColor = if (isBold) boldTextColor else baseTextColor
				val glyph = font.getGlyphForChar(nextChar)
				textBatch.glyphAt(
					textX, textY, font, textHeight, glyph,
					textColor, strokeColor, strokeWidth
				)

				textX += textHeight * font.getGlyphAdvance(glyph)

				remaining -= 1f
			}
		}
	}
}
