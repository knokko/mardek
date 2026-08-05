package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.renderer.area.AreaRenderContext
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderBoxButton
import mardek.renderer.util.renderDescription
import mardek.state.ingame.area.AreaSuspensionOpeningChest
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun renderChestLoot(areaContext: AreaRenderContext, opacity: Float) {
	areaContext.run {
		val suspension = state.suspension
		val obtainedItemStack = if (suspension is AreaSuspensionOpeningChest) suspension.obtainedItem else null
		if (obtainedItemStack == null) return

		val sprite = if (obtainedItemStack.itemStack != null) obtainedItemStack.itemStack!!.item.sprite
		else obtainedItemStack.plotItem!!.sprite

		val alpha = (255f * opacity).roundToInt()
		val multiplyColor = rgba(255, 255, 255, alpha)

		val rectWidth = 120 * scale
		val rectHeight = 90 * scale
		val rectMinX = region.minX + (region.width - rectWidth) / 2
		val rectMinY = region.minY + (region.height - rectHeight) / 3
		val rectMaxX = rectMinX + rectWidth - 1
		val rectMaxY = rectMinY + rectHeight - 1

		val columnWidth = 35 * scale

		val spriteBatch2 = context.addAreaSpriteBatch(10, scissor)
		spriteBatch2.draw(sprite, rectMinX - 25 * scale, rectMinY + 2 * scale, scale, opacity = opacity)

		run {
			val text = if (obtainedItemStack.itemStack != null) "TREASURE!!" else "PLOT ITEM!!!"
			val font = context.bundle.getFont(context.content.fonts.basic2.index)
			for (style in arrayOf(MardekTextStyles.ChestLoot.titleBack(alpha), MardekTextStyles.ChestLoot.titleFront(alpha))) {
				fancyTextBatch.drawString(
					text, rectMinX + 1f * scale, rectMinY - 4f * scale, 0f,
					8f * scale, font, style, TextAlignment.LEFT,
				)
			}
		}

		if (obtainedItemStack.itemStack != null) {
			for ((column, character) in obtainedItemStack.usedParty) {
				val spriteIndex = context.timing.walkingSpriteIndex()
				spriteBatch2.draw(
					character.areaSprites.sprites[spriteIndex],
					rectMinX + scale + column * columnWidth,
					y = rectMaxY + 5 * scale, scale, opacity = opacity
				)
			}
		}

		val leftColor = srgbToLinear(rgba(25, 15, 11, alpha))
		val rightColor = srgbToLinear(rgba(107, 88, 50, alpha))
		val upColor = srgbToLinear(rgba(58, 48, 43, alpha))
		val outerBorderWidth = max(1, region.height / 400)
		gradientWithBorder(
			uiColorBatch, rectMinX, rectMinY, rectMaxX, rectMaxY, outerBorderWidth, outerBorderWidth,
			srgbToLinear(rgba(208, 193, 142, alpha)),
			leftColor, rightColor, upColor
		)
		val innerBorderWidth = 2 * scale
		uiColorBatch.fill(
			rectMinX + outerBorderWidth, rectMinY + innerBorderWidth + outerBorderWidth,
			rectMinX + innerBorderWidth + outerBorderWidth - 1, rectMaxY - outerBorderWidth, leftColor
		)
		uiColorBatch.fill(
			rectMaxX - innerBorderWidth - outerBorderWidth, rectMinY + innerBorderWidth + outerBorderWidth,
			rectMaxX - outerBorderWidth, rectMaxY - outerBorderWidth, rightColor
		)
		uiColorBatch.gradient(
			rectMinX + outerBorderWidth, rectMinY + outerBorderWidth,
			rectMaxX - outerBorderWidth, rectMinY + innerBorderWidth + outerBorderWidth - 1,
			leftColor, rightColor, leftColor
		)

		if (obtainedItemStack.plotItem != null) {
			val goldColor = MardekTextStyles.ChestLoot.gainedAmount(alpha, true).mainStyle.fill.color
			val margin = 2 * scale
			uiColorBatch.fill(
				rectMinX - margin, rectMinY - margin,
				rectMaxX + margin, rectMinY - margin, goldColor
			)
			uiColorBatch.fill(
				rectMinX - margin, rectMaxY + margin,
				rectMaxX + margin, rectMaxY + margin, goldColor
			)
			uiColorBatch.fill(
				rectMinX - margin, rectMinY - margin,
				rectMinX - margin, rectMaxY + margin, goldColor
			)
			uiColorBatch.fill(
				rectMaxX + margin, rectMinY - margin,
				rectMaxX + margin, rectMaxY + margin, goldColor
			)
		}

		val minTextX = rectMinX + 7 * scale
		val (itemName, description) = if (obtainedItemStack.itemStack != null) {
			Pair(obtainedItemStack.itemStack!!.item.displayName, obtainedItemStack.itemStack!!.item.description)
		} else Pair(obtainedItemStack.plotItem!!.displayName, obtainedItemStack.plotItem!!.description)

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		simpleTextBatch.drawShadowedString(
			itemName, minTextX.toFloat(), rectMinY + 9f * scale, 5f * scale,
			font, MardekTextStyles.ChestLoot.itemName(alpha, obtainedItemStack.plotItem != null),
			TextAlignment.LEFT
		)

		var textY = rectMinY + 20 * scale

		fun drawLine(currentLine: String) {
			simpleTextBatch.drawShadowedString(
				currentLine, minTextX.toFloat(), textY.toFloat(), 4f * scale, font,
				MardekTextStyles.ChestLoot.description(alpha), TextAlignment.LEFT
			)
			@Suppress("AssignedValueIsNeverRead")
			textY += 8 * scale
		}

		renderDescription(description, 42, ::drawLine)

		if (obtainedItemStack.itemStack != null) {
			simpleTextBatch.drawShadowedString(
				"x ${obtainedItemStack.itemStack!!.amount}",
				rectMaxX + 4f * scale, rectMaxY - scale.toFloat(), 8f * scale, font,
				MardekTextStyles.ChestLoot.gainedAmount(alpha, obtainedItemStack.plotItem != null),
				TextAlignment.LEFT
			)

			for ((column, _, characterState) in obtainedItemStack.usedParty) {
				val minX = rectMinX + columnWidth * column
				if (column == obtainedItemStack.partyIndex) {
					val borderColor = srgbToLinear(rgba(165, 205, 254, alpha))
					val lowColor = srgbToLinear(rgba(19, 65, 114, alpha))
					gradientWithBorder(
						uiColorBatch, minX, rectMaxY + 3 * scale,
						minX + 18 * scale + 1, rectMaxY + 21 * scale + 1,
						outerBorderWidth, outerBorderWidth,
						borderColor, lowColor, lowColor, 0
					)
				}

				val alreadyHas = characterState.countItemOccurrences(obtainedItemStack.itemStack!!.item)
				simpleTextBatch.drawShadowedString(
					alreadyHas.toString(), minX + 9f * scale, rectMaxY + 32f * scale,
					6f * scale, font,
					MardekTextStyles.ChestLoot.alreadyHasAmount(alpha, obtainedItemStack.plotItem != null),
					TextAlignment.CENTERED
				)
			}

			simpleTextBatch.drawString(
				"Already has:", rectMinX - 2f * scale, rectMaxY + 32f * scale,
				4f * scale, font, MardekTextStyles.ChestLoot.infoLabelText(alpha), TextAlignment.RIGHT
			)
			simpleTextBatch.drawString(
				"Space:", rectMinX - 2f * scale, rectMaxY + 43f * scale,
				4f * scale, font, MardekTextStyles.ChestLoot.infoLabelText(alpha), TextAlignment.RIGHT
			)
		}

		if (obtainedItemStack.itemStack != null) {
			val minY = rectMaxY + 40 * scale

			for (entry in context.campaign.usedPartyMembers()) {
				val minX = rectMinX + scale + entry.index * columnWidth
				uiColorBatch.fill(
					minX, minY, minX + 16 * scale - 1, minY + 16 * scale - 1,
					srgbToLinear(rgba(24, 15, 10, alpha))
				)
			}

			renderLootInventoryGrid(
				uiColorBatch, obtainedItemStack.usedParty, rectMinX + scale, minY, columnWidth, 2 * scale
			)

			val arrowPeriod = 750.milliseconds
			val arrowSprite = context.content.ui.arrowHead
			val arrowScale = 8f * scale / arrowSprite.height
			actionsImageBatch.rotated(
				rectMinX - scale * context.timing.oscillate(8f, 10f, arrowPeriod),
				rectMaxY + 12f * scale,
				180f, arrowScale, arrowSprite.index, 0, multiplyColor,
			)
			actionsImageBatch.rotated(
				rectMaxX + scale * context.timing.oscillate(8f, 10f, arrowPeriod),
				rectMaxY + 12f * scale,
				0f, arrowScale, arrowSprite.index, 0, multiplyColor,
			)
		}

		val minBoxSize = 14f * scale
		val maxBoxSize = 16f * scale
		val floatBoxSize = context.timing.oscillate(minBoxSize, maxBoxSize, 1.seconds)
		val boxSize = floatBoxSize.roundToInt()
		val boxOffset = (minBoxSize + 0.5f * (boxSize - minBoxSize)).roundToInt()
		val boxX = rectMaxX + 16 * scale - boxOffset
		val boxY = rectMaxY + 36 * scale - boxOffset
		renderBoxButton(
			uiColorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, context.bundle, context.content.fonts,
			minBoxSize, boxSize, boxX, boxY, alpha = alpha,
		)
	}
}
