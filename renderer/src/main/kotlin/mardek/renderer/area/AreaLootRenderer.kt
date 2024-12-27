package mardek.renderer.area

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.assets.ui.UiSprites
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimRequest
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.area.loot.ObtainedItemStack

private val referenceTime = System.nanoTime()

class AreaLootRenderer(
	private val ui: UiSprites,
	private val obtainedItemStack: ObtainedItemStack,
	private val resources: SharedResources,
	private val scale: Int,
	targetImage: VkbImage,
) {

	private val kimBatch = resources.kim1Renderer.startBatch()

	private val rectWidth = 120 * scale
	private val rectHeight = 90 * scale
	private val rectMinX = (targetImage.width - rectWidth) / 2
	private val rectMinY = (targetImage.height - rectHeight) / 3
	private val rectMaxX = rectMinX + rectWidth - 1
	private val rectMaxY = rectMinY + rectHeight - 1

	private val columnWidth = 35 * scale

	fun beforeRendering() {
		val sprite = if (obtainedItemStack.itemStack != null) obtainedItemStack.itemStack!!.item.sprite
		else obtainedItemStack.plotItem!!.sprite
		kimBatch.requests.add(KimRequest(
			x = rectMinX - 25 * scale, y = rectMinY + 2 * scale,
			scale = scale.toFloat(), sprite = sprite, opacity = 1f
		))

		if (obtainedItemStack.itemStack != null) {
			kimBatch.requests.add(KimRequest(
				x = rectMinX, y = rectMinY - 10 * scale, scale = scale / 2.5f,
				sprite = ui.treasure, opacity = 1f
			))

			for ((column, character) in obtainedItemStack.party.withIndex()) {
				if (character == null) continue

				var spriteIndex = 0
				val passedTime = System.nanoTime() - referenceTime
				val animationPeriod = 700_000_000L
				if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

				kimBatch.requests.add(KimRequest(
					x = rectMinX + scale + column * columnWidth, y = rectMaxY + 5 * scale, scale = scale.toFloat(),
					sprite = character.areaSprites.sprites[spriteIndex], opacity = 1f
				))
			}
		} else {
			kimBatch.requests.add(KimRequest(
				x = rectMinX, y = rectMinY - 13 * scale, scale = scale / 2.5f,
				sprite = ui.plotItem, opacity = 1f
			))
		}
	}

	fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val uiRenderer = resources.uiRenderers[frameIndex]
		uiRenderer.beginBatch()
		uiRenderer.fillColor(
			0, 0, targetImage.width, targetImage.height, srgbToLinear(rgba(37, 26, 17, 254))
		)

		val leftColor = srgbToLinear(rgb(25, 15, 11))
		val rightColor = srgbToLinear(rgb(107, 88, 50))
		val upColor = srgbToLinear(rgb(58, 48, 43))
		uiRenderer.fillColor(
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
			uiRenderer.fillColor(
				rectMinX - margin, rectMinY - margin, rectMaxX + margin, rectMinY - margin, goldColor
			)
			uiRenderer.fillColor(
				rectMinX - margin, rectMaxY + margin, rectMaxX + margin, rectMaxY + margin, goldColor
			)
			uiRenderer.fillColor(
				rectMinX - margin, rectMinY - margin, rectMinX - margin, rectMaxY + margin, goldColor
			)
			uiRenderer.fillColor(
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
		uiRenderer.drawString(
			resources.font, itemName, brightTextColor, intArrayOf(),
			minTextX, 0, maxTextX, targetImage.height, rectMinY + 9 * scale, 5 * scale,
			1, TextAlignment.DEFAULT
		)

		var textY = rectMinY + 20 * scale

		fun drawLine(currentLine: String) {
			uiRenderer.drawString(
				resources.font, currentLine, srgbToLinear(rgb(197, 183, 134)), intArrayOf(),
				minTextX, 0, maxTextX, targetImage.height, textY, 4 * scale, 1, TextAlignment.DEFAULT
			)
			textY += 8 * scale
		}

		renderDescription(description, 45, ::drawLine)

		if (obtainedItemStack.itemStack != null) {
			uiRenderer.drawString(
				resources.font, "x ${obtainedItemStack.itemStack!!.amount}", brightTextColor, intArrayOf(),
				rectMaxX + 4 * scale, 0, targetImage.width, targetImage.height,
				rectMaxY - scale, 8 * scale, 1, TextAlignment.LEFT
			)

			for ((column, character) in obtainedItemStack.party.withIndex()) {
				if (character == null) continue

				val minX = rectMinX + columnWidth * column
				if (column == obtainedItemStack.partyIndex) {
					val borderColor = srgbToLinear(rgb(99, 128, 177))
					val lowColor = srgbToLinear(rgb(19, 65, 114))
					uiRenderer.fillColor(
						minX, rectMaxY + 3 * scale, minX + 18 * scale + 1, rectMaxY + 21 * scale + 1, borderColor,
						Gradient(1, 1, 18 * scale, 18 * scale, lowColor, lowColor, 0)
					)
				}

				val characterState = obtainedItemStack.characters[character]!!
				var alreadyHas = 0
				for (item in characterState.equipment) {
					if (item == obtainedItemStack.itemStack!!.item) alreadyHas += 1
				}
				for (itemStack in characterState.inventory) {
					if (itemStack != null && itemStack.item == obtainedItemStack.itemStack!!.item) {
						alreadyHas += itemStack.amount
					}
				}
				uiRenderer.drawString(
					resources.font, alreadyHas.toString(), brightTextColor, intArrayOf(),
					minX, rectMaxY, minX + 18 * scale, targetImage.height,
					rectMaxY + 32 * scale, 6 * scale, 1, TextAlignment.CENTER
				)
			}

			uiRenderer.drawString(
				resources.font, "Already has:", brightTextColor, intArrayOf(),
				0, rectMaxY, rectMinX - 2 * scale, targetImage.height,
				rectMaxY + 32 * scale, 4 * scale, 1, TextAlignment.RIGHT
			)
			uiRenderer.drawString(
				resources.font, "Space:", brightTextColor, intArrayOf(),
				0, rectMaxY, rectMinX - 2 * scale, targetImage.height,
				rectMaxY + 43 * scale, 4 * scale, 1, TextAlignment.RIGHT
			)
		}

		uiRenderer.endBatch()

		if (obtainedItemStack.itemStack != null) {
			resources.colorGridRenderer.startBatch(recorder)
			for ((column, character) in obtainedItemStack.party.withIndex()) {
				if (character == null) continue
				val inventory = obtainedItemStack.characters[character]!!.inventory
				if (inventory.size % 8 != 0) throw Error("Huh? inventory size is ${inventory.size}")

				val minX = rectMinX + columnWidth * column + scale
				val colorIndexBuffer = resources.colorGridRenderer.drawGrid(
					recorder, targetImage, minX, rectMaxY + 40 * scale,
					8, inventory.size / 8, 0, 2 * scale
				)

				for (row in 0 until inventory.size / 8) {
					var indices = 0u
					for (itemColumn in 0 until 8) {
						var localBits = 0u
						val itemStack = inventory[itemColumn + 8 * row]
						if (itemStack != null) {
							val item = itemStack.item
							localBits = 1u

							if (item.consumable != null) localBits = 2u
							val equipment = item.equipment
							if (equipment != null) {
								localBits = 5u
								if (equipment.weapon != null) localBits = 3u
								if (equipment.armorType != null) localBits = 4u
							}
						}
						indices = indices or (localBits shl (4 * itemColumn))
					}
					colorIndexBuffer.put(indices.toInt())
				}
			}
			resources.colorGridRenderer.endBatch()
		}

		resources.kim1Renderer.submit(kimBatch, recorder, targetImage)
	}
}
