package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.InGameRenderContext
import mardek.renderer.batch.KimRequest
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.area.loot.ObtainedItemStack

private val referenceTime = System.nanoTime()

class AreaLootRenderer(
	private val context: InGameRenderContext,
	private val obtainedItemStack: ObtainedItemStack,
	private val scale: Int,
) {

	private val kimBatch = context.resources.kim1Renderer.startBatch()

	private val rectWidth = 120 * scale
	private val rectHeight = 90 * scale
	private val rectMinX = (context.targetImage.width - rectWidth) / 2
	private val rectMinY = (context.targetImage.height - rectHeight) / 3
	private val rectMaxX = rectMinX + rectWidth - 1
	private val rectMaxY = rectMinY + rectHeight - 1

	private val columnWidth = 35 * scale

	fun beforeRendering() {
		val sprite = if (obtainedItemStack.itemStack != null) obtainedItemStack.itemStack!!.item.sprite
		else obtainedItemStack.plotItem!!.sprite
		kimBatch.requests.add(KimRequest(
			x = rectMinX - 25 * scale, y = rectMinY + 2 * scale,
			scale = scale.toFloat(), sprite = sprite
		))

		if (obtainedItemStack.itemStack != null) {
			kimBatch.requests.add(KimRequest(
				x = rectMinX, y = rectMinY - 10 * scale, scale = scale / 2.5f,
				sprite = context.content.ui.treasure
			))

			for ((column, character) in obtainedItemStack.party.withIndex()) {
				if (character == null) continue

				var spriteIndex = 0
				val passedTime = System.nanoTime() - referenceTime
				val animationPeriod = 700_000_000L
				if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

				kimBatch.requests.add(KimRequest(
					x = rectMinX + scale + column * columnWidth, y = rectMaxY + 5 * scale, scale = scale.toFloat(),
					sprite = character.areaSprites.sprites[spriteIndex]
				))
			}
		} else {
			kimBatch.requests.add(KimRequest(
				x = rectMinX, y = rectMinY - 13 * scale, scale = scale / 2.5f,
				sprite = context.content.ui.plotItem
			))
		}
	}

	fun render() {
		context.uiRenderer.beginBatch()
		context.uiRenderer.fillColor(
			0, 0, context.targetImage.width, context.targetImage.height,
			srgbToLinear(rgba(37, 26, 17, 254))
		)

		val leftColor = srgbToLinear(rgb(25, 15, 11))
		val rightColor = srgbToLinear(rgb(107, 88, 50))
		val upColor = srgbToLinear(rgb(58, 48, 43))
		context.uiRenderer.fillColor(
			rectMinX, rectMinY, rectMaxX, rectMaxY, srgbToLinear(rgb(155, 138, 95)),
			Gradient(1, 1, rectWidth - 2, rectHeight - 2, leftColor, rightColor, leftColor),
			Gradient(
				2 + scale, 2 + scale, rectWidth - 4 - 2 * scale, rectHeight - 3 - scale,
				leftColor, rightColor, upColor
			)
		)

		val goldColor = srgbToLinear(rgb(255, 204, 0))
		if (obtainedItemStack.plotItem != null) {
			val margin = 2 * scale
			context.uiRenderer.fillColor(
				rectMinX - margin, rectMinY - margin, rectMaxX + margin, rectMinY - margin, goldColor
			)
			context.uiRenderer.fillColor(
				rectMinX - margin, rectMaxY + margin, rectMaxX + margin, rectMaxY + margin, goldColor
			)
			context.uiRenderer.fillColor(
				rectMinX - margin, rectMinY - margin, rectMinX - margin, rectMaxY + margin, goldColor
			)
			context.uiRenderer.fillColor(
				rectMaxX + margin, rectMinY - margin, rectMaxX + margin, rectMaxY + margin, goldColor
			)
		}

		val minTextX = rectMinX + 7 * scale
		val maxTextX = rectMaxX - 7 * scale
		var brightTextColor = srgbToLinear(rgb(238, 203, 127))
		if (obtainedItemStack.plotItem != null) brightTextColor = goldColor
		val (itemName, description) = if (obtainedItemStack.itemStack != null) {
			Pair(obtainedItemStack.itemStack!!.item.flashName, obtainedItemStack.itemStack!!.item.description)
		} else Pair(obtainedItemStack.plotItem!!.name, obtainedItemStack.plotItem!!.description)
		context.uiRenderer.drawString(
			context.resources.font, itemName, brightTextColor, intArrayOf(),
			minTextX, 0, maxTextX, context.targetImage.height, rectMinY + 9 * scale, 5 * scale,
			1, TextAlignment.DEFAULT
		)

		var textY = rectMinY + 20 * scale

		fun drawLine(currentLine: String) {
			context.uiRenderer.drawString(
				context.resources.font, currentLine, srgbToLinear(rgb(197, 183, 134)), intArrayOf(),
				minTextX, 0, maxTextX, context.targetImage.height, textY, 4 * scale, 1, TextAlignment.DEFAULT
			)
			textY += 8 * scale
		}

		renderDescription(description, 45, ::drawLine)

		if (obtainedItemStack.itemStack != null) {
			context.uiRenderer.drawString(
				context.resources.font, "x ${obtainedItemStack.itemStack!!.amount}", brightTextColor, intArrayOf(),
				rectMaxX + 4 * scale, 0, context.targetImage.width, context.targetImage.height,
				rectMaxY - scale, 8 * scale, 1, TextAlignment.LEFT
			)

			for ((column, character) in obtainedItemStack.party.withIndex()) {
				if (character == null) continue

				val minX = rectMinX + columnWidth * column
				if (column == obtainedItemStack.partyIndex) {
					val borderColor = srgbToLinear(rgb(99, 128, 177))
					val lowColor = srgbToLinear(rgb(19, 65, 114))
					context.uiRenderer.fillColor(
						minX, rectMaxY + 3 * scale, minX + 18 * scale + 1, rectMaxY + 21 * scale + 1, borderColor,
						Gradient(1, 1, 18 * scale, 18 * scale, lowColor, lowColor, 0)
					)
				}

				val characterState = obtainedItemStack.characters[character]!!
				val alreadyHas = characterState.countItemOccurrences(obtainedItemStack.itemStack!!.item)
				context.uiRenderer.drawString(
					context.resources.font, alreadyHas.toString(), brightTextColor, intArrayOf(),
					minX, rectMaxY, minX + 18 * scale, context.targetImage.height,
					rectMaxY + 32 * scale, 6 * scale, 1, TextAlignment.CENTER
				)
			}

			context.uiRenderer.drawString(
				context.resources.font, "Already has:", brightTextColor, intArrayOf(),
				0, rectMaxY, rectMinX - 2 * scale, context.targetImage.height,
				rectMaxY + 32 * scale, 4 * scale, 1, TextAlignment.RIGHT
			)
			context.uiRenderer.drawString(
				context.resources.font, "Space:", brightTextColor, intArrayOf(),
				0, rectMaxY, rectMinX - 2 * scale, context.targetImage.height,
				rectMaxY + 43 * scale, 4 * scale, 1, TextAlignment.RIGHT
			)
		}

		context.uiRenderer.endBatch()

		if (obtainedItemStack.itemStack != null) {
			val party = obtainedItemStack.party.map { if (it != null) obtainedItemStack.characters[it]!! else null }
			val minY = rectMaxY + 40 * scale
			renderLootInventoryGrid(context, party, rectMinX + scale, minY, columnWidth, scale)
		}

		context.resources.kim1Renderer.submit(kimBatch, context.recorder, context.targetImage)
	}
}
