package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderDescription

private val referenceTime = System.nanoTime()

internal fun renderChestLoot(areaContext: AreaRenderContext) {
	areaContext.run {
		val obtainedItemStack = state.obtainedItemStack ?: return
		val sprite = if (obtainedItemStack.itemStack != null) obtainedItemStack.itemStack!!.item.sprite
		else obtainedItemStack.plotItem!!.sprite

		val rectWidth = 120 * scale
		val rectHeight = 90 * scale
		val rectMinX = region.minX + (region.width - rectWidth) / 2
		val rectMinY = region.minY + (region.height - rectHeight) / 3
		val rectMaxX = rectMinX + rectWidth - 1
		val rectMaxY = rectMinY + rectHeight - 1

		val columnWidth = 35 * scale

		val spriteBatch2 = context.addAreaSpriteBatch(10, scissor)
		spriteBatch2.draw(sprite, rectMinX - 25 * scale, rectMinY + 2 * scale, scale)

		run {
			val text = if (obtainedItemStack.itemStack != null) "TREASURE!!" else "PLOT ITEM!!!"
			val lowColor = srgbToLinear(rgb(204, 153, 0))
			val highColor = srgbToLinear(rgb(255, 204, 102))
			val strokeColor = srgbToLinear(rgb(132, 81, 37))
			val font = context.bundle.getFont(context.content.fonts.basic2.index)
			textBatch.drawFancyString(
				text, rectMinX + 2f * scale, rectMinY - 2f * scale, 8f * scale,
				font, lowColor, strokeColor, 2.5f * scale, TextAlignment.LEFT,
				lowColor, highColor, highColor, highColor,
				0.5f, 0.5f, 0.5f, 0.5f
			)
		}

		if (obtainedItemStack.itemStack != null) {
			for ((column, character) in obtainedItemStack.usedParty) {
				var spriteIndex = 0
				val passedTime = System.nanoTime() - referenceTime
				val animationPeriod = 700_000_000L
				if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1
				spriteBatch2.draw(
					character.areaSprites.sprites[spriteIndex],
					rectMinX + scale + column * columnWidth,
					y = rectMaxY + 5 * scale, scale
				)
			}
		}

		colorBatch.fill(
			region.minX, region.minY, region.maxX, region.maxY,
			srgbToLinear(rgba(37, 26, 17, 254))
		)

		val leftColor = srgbToLinear(rgb(25, 15, 11))
		val rightColor = srgbToLinear(rgb(107, 88, 50))
		val upColor = srgbToLinear(rgb(58, 48, 43))
		gradientWithBorder(
			colorBatch, rectMinX, rectMinY, rectMaxX, rectMaxY, 1, 1,
			srgbToLinear(rgb(155, 138, 95)),
			leftColor, rightColor, upColor
		)

		val goldColor = srgbToLinear(rgb(255, 204, 0))
		if (obtainedItemStack.plotItem != null) {
			val margin = 2 * scale
			colorBatch.fill(
				rectMinX - margin, rectMinY - margin,
				rectMaxX + margin, rectMinY - margin, goldColor
			)
			colorBatch.fill(
				rectMinX - margin, rectMaxY + margin,
				rectMaxX + margin, rectMaxY + margin, goldColor
			)
			colorBatch.fill(
				rectMinX - margin, rectMinY - margin,
				rectMinX - margin, rectMaxY + margin, goldColor
			)
			colorBatch.fill(
				rectMaxX + margin, rectMinY - margin,
				rectMaxX + margin, rectMaxY + margin, goldColor
			)
		}

		val minTextX = rectMinX + 7 * scale
		var brightTextColor = srgbToLinear(rgb(238, 203, 127))
		val simpleTextColor = srgbToLinear(rgb(208, 193, 142))
		if (obtainedItemStack.plotItem != null) brightTextColor = goldColor
		val (itemName, description) = if (obtainedItemStack.itemStack != null) {
			Pair(obtainedItemStack.itemStack!!.item.flashName, obtainedItemStack.itemStack!!.item.description)
		} else Pair(obtainedItemStack.plotItem!!.name, obtainedItemStack.plotItem!!.description)

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		textBatch.drawShadowedString(
			itemName, minTextX.toFloat(), rectMinY + 9f * scale, 5f * scale,
			font, brightTextColor, 0, 0f, rgb(0, 0, 0),
			0.6f * scale, 0.6f * scale, TextAlignment.LEFT
		)

		var textY = rectMinY + 20 * scale

		fun drawLine(currentLine: String) {
			textBatch.drawShadowedString(
				currentLine, minTextX.toFloat(), textY.toFloat(), 4f * scale, font,
				simpleTextColor, 0, 0f, rgb(0, 0, 0),
				0.5f * scale, 0.5f * scale, TextAlignment.LEFT
			)
			@Suppress("AssignedValueIsNeverRead")
			textY += 8 * scale
		}

		renderDescription(description, 42, ::drawLine)

		if (obtainedItemStack.itemStack != null) {
			textBatch.drawShadowedString(
				"x ${obtainedItemStack.itemStack!!.amount}",
				rectMaxX + 4f * scale, rectMaxY - scale.toFloat(), 8f * scale, font,
				brightTextColor, 0, 0f, rgb(0, 0, 0),
				0.8f * scale, 0.8f * scale, TextAlignment.LEFT
			)

			for ((column, _, characterState) in obtainedItemStack.usedParty) {
				val minX = rectMinX + columnWidth * column
				if (column == obtainedItemStack.partyIndex) {
					val borderColor = srgbToLinear(rgb(99, 128, 177))
					val lowColor = srgbToLinear(rgb(19, 65, 114))
					gradientWithBorder(
						colorBatch, minX, rectMaxY + 3 * scale,
						minX + 18 * scale + 1, rectMaxY + 21 * scale + 1,
						1, 1, borderColor, lowColor, lowColor, 0
					)
				}

				val alreadyHas = characterState.countItemOccurrences(obtainedItemStack.itemStack!!.item)
				textBatch.drawShadowedString(
					alreadyHas.toString(), minX + 9f * scale, rectMaxY + 32f * scale, 6f * scale,
					font, brightTextColor, 0, 0f, rgb(0, 0, 0),
					0.8f * scale, 0.8f * scale, TextAlignment.CENTERED
				)
			}

			textBatch.drawString(
				"Already has:", rectMinX - 2 * scale, rectMaxY + 32 * scale,
				4 * scale, font, simpleTextColor, TextAlignment.RIGHT
			)
			textBatch.drawString(
				"Space:", rectMinX - 2 * scale, rectMaxY + 43 * scale,
				4 * scale, font, simpleTextColor, TextAlignment.RIGHT
			)
		}

		if (obtainedItemStack.itemStack != null) {
			val minY = rectMaxY + 40 * scale
			renderLootInventoryGrid(
				colorBatch, obtainedItemStack.usedParty, rectMinX + scale, minY, columnWidth, 2 * scale
			)
		}
	}
}
