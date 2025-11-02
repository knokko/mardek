package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.characters.PlayableCharacter
import mardek.renderer.RenderContext
import mardek.renderer.area.renderLootInventoryGrid
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderButton
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.util.Rectangle
import kotlin.math.max

private val referenceTime = System.nanoTime()

internal fun renderBattleLoot(
	context: RenderContext, loot: BattleLoot,
	party: Array<PlayableCharacter?>, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val colorBatch = context.addColorBatch(1000)
	val ovalBatch = context.addOvalBatch(20)
	val kimBatch = context.addKim3Batch(100)
	val imageBatch = context.addImageBatch(2)
	val textBatch = context.addFancyTextBatch(1000)
	val itemYs = IntArray(loot.items.size + loot.plotItems.size + loot.dreamStones.size)

	val scale = max(1, region.height / (11 * 16))
	val partyMinX = region.boundX - 5 * scale - 18 * scale * party.size

	for ((column, character) in party.withIndex()) {
		if (character == null) continue

		var spriteIndex = 0
		val passedTime = System.nanoTime() - referenceTime
		val animationPeriod = 700_000_000L
		if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

		kimBatch.simple(
			partyMinX + column * 18 * scale, region.minY + 5 * scale,
			scale, character.areaSprites.sprites[spriteIndex].index,
		)
	}

	val selectedElement = loot.selectedElement
	val pointer = context.content.ui.pointer
	val pointerScale = 6f * scale / pointer.height
	val rowHeight = 18 * scale
	val itemX = region.minX + 30 * scale
	var itemY = region.minY + 40 * scale
	var row = 0
	for (item in loot.items) {
		kimBatch.simple(
			itemX - 16 * scale, itemY, scale,
			item.item.sprite.index,
		)
		if (selectedElement is BattleLoot.SelectedItem && row == selectedElement.index) {
			imageBatch.simpleScale(
				itemX - 25f * scale, itemY + 5f * scale,
				pointerScale, pointer.index,
			)
		}
		// TODO Get rid of itemYs
		itemYs[row] = itemY
		row += 1
		itemY += rowHeight
	}
	if (selectedElement is BattleLoot.SelectedGetAll) {
		imageBatch.simpleScale(
			region.minX + 3f * scale, region.minY + 28f * scale,
			pointerScale, pointer.index,
		)
	}
	if (selectedElement is BattleLoot.SelectedFinish) {
		imageBatch.simpleScale(
			region.minX + 3f * scale, region.boundY - 33f * scale,
			pointerScale, pointer.index,
		)
	}
	for (plotItem in loot.plotItems) {
		kimBatch.simple(itemX - 16 * scale, itemY, scale, plotItem.sprite.index)
		itemYs[row] = itemY
		row += 1
		itemY += rowHeight
	}
	repeat(loot.dreamStones.size) {
		kimBatch.simple(
			itemX - 16 * scale, itemY, scale,
			context.content.ui.dreamStoneIcon.index,
		)
		itemYs[row] = itemY
		row += 1
		itemY += rowHeight
	}
	kimBatch.simple(
		region.minX + 8 * scale, region.boundY - 20 * scale,
		scale, context.content.ui.goldIcon.index,
	)

	val leftColor = srgbToLinear(rgba(54, 37, 21, 240))
	val rightColor = srgbToLinear(rgba(132, 84, 53, 240))
	val x1 = partyMinX - 20 * scale
	val x2 = partyMinX - 2 * scale
	val y1 = region.minY + 12 * scale
	val y2 = region.minY + 24 * scale
	// TODO This one was moved to InGameRenderer blur filter. Fix colors later
//	colorBatch.gradient(
//		region.minX, region.minY, region.maxX, region.maxY,
//		leftColor, rightColor, leftColor
//	)

	// Render dark top bar & bottom bar
	val upColor = changeAlpha(rightColor, 100)
	colorBatch.fill(region.minX, y1, region.maxX, y2, upColor)
	val barColor = rgb(0, 0, 0)
	colorBatch.fill(region.minX, region.minY, x1, y1, barColor)
	colorBatch.fillUnaligned(x1, y1, x2, y2, x2, region.minY, x1, region.minY, barColor)
	colorBatch.fill(x2, region.minY, region.maxX, y2, barColor)

	val x4 = region.boundX - 55 * scale
	val x3 = x4 - 4 * scale
	val y4 = region.boundY - 25 * scale
	val y3 = y4 - 5 * scale
	colorBatch.fill(region.minX, y4, region.maxX, region.maxY, barColor)
	colorBatch.fillUnaligned(x3, y4, region.boundX, y4, region.boundX, y3, x4, y3, barColor)

	for (indexY in loot.items.indices) {
		if (selectedElement is BattleLoot.SelectedItem && selectedElement.index == indexY) {
			val color = srgbToLinear(rgb(51, 102, 204))
			val itemY = itemYs[indexY]
			colorBatch.fill(
				region.minX, itemY, region.maxX, itemY + 16 * scale,
				changeAlpha(color, 50)
			)
			colorBatch.fill(region.minX, itemY, region.maxX, itemY + 1, color)
			colorBatch.fill(
				region.minX, itemY + 16 * scale - 1,
				region.maxX, itemY + 16 * scale, color
			)
		}
	}

	val goldBackground = srgbToLinear(rgb(35, 23, 15))
	val minGoldY = region.boundY - 21 * scale
	val maxGoldY = region.boundY - 3 * scale
	val goldCircleX = region.minX + region.width / 2
	val goldRadius = (maxGoldY - minGoldY) / 2
	colorBatch.fill(region.minX, minGoldY, goldCircleX, maxGoldY, goldBackground)

	val unknownFont = context.bundle.getFont(context.content.fonts.basic2.index)
	textBatch.drawString(
		"INVENTORY SPACE", x4, y4, 4 * scale, unknownFont,
		srgbToLinear(rgb(131, 81, 37)),
	)
//	context.uiRenderer.drawString(
//		context.resources.font, "INVENTORY SPACE", ,
//		IntArray(0), x4, y3, width - 1, height - 1,
//		y4, 4 * scale, 1, TextAlignment.LEFT
//	)

	// Render "Spoils" and the random title
	textBatch.drawString(
		"Spoils", region.minX + region.width / 50, region.minY + 9 * scale, 6 * scale,
		unknownFont, srgbToLinear(rgb(131, 81, 37)),
	)
//	context.uiRenderer.drawString(
//		context.resources.font, "Spoils", ,
//		IntArray(0), width / 50, 0, width / 3, height / 3,
//		9 * scale, 6 * scale, 1, TextAlignment.LEFT
//	)
	textBatch.drawString(
		loot.itemText, region.minX + region.width / 50, region.minY + 20 * scale, 4 * scale,
		unknownFont, srgbToLinear(rgb(207, 192, 141)),
	)
//	context.uiRenderer.drawString(
//		context.resources.font, loot.itemText, srgbToLinear(rgb(207, 192, 141)),
//		IntArray(0), width / 50, 0, width / 2, height / 3,
//		20 * scale, 4 * scale, 1, TextAlignment.LEFT
//	)

	// Render item rows
	run {
		val textHeight = 6 * scale
		var indexY = 0
		for (itemStack in loot.items) {
			var strongColor = srgbToLinear(rgb(238, 203, 127))
			var weakColor = srgbToLinear(rgb(192, 144, 89))
			if (selectedElement is BattleLoot.SelectedItem && selectedElement.index == indexY) {
				strongColor = srgbToLinear(rgb(152, 203, 255))
				weakColor = srgbToLinear(rgb(51, 102, 255))
			}
			val textY = itemYs[indexY] + 10 * scale
			textBatch.drawString(
				itemStack.item.flashName, region.minX + 32 * scale, textY,
				textHeight, unknownFont, strongColor,
			)
//			context.uiRenderer.drawString(
//				context.resources.font, itemStack.item.flashName, strongColor,
//				IntArray(0), 32 * scale, 0, width / 2, height,
//				textY, textHeight, 1, TextAlignment.LEFT
//			)
			textBatch.drawString(
				"x ${itemStack.amount}", region.minX + 140 * scale, textY,
				textHeight, unknownFont, strongColor,
			)
//			context.uiRenderer.drawString(
//				context.resources.font, "x ${itemStack.amount}", strongColor,
//				IntArray(0), 140 * scale, 0, width, height,
//				textY, textHeight, 1, TextAlignment.LEFT
//			)
			for ((column, member) in party.withIndex()) {
				if (member == null) continue
				val currentAmount = context.campaign.characterStates[member]!!.countItemOccurrences(itemStack.item)
				textBatch.drawString(
					currentAmount.toString(), partyMinX + 5 * scale + 18 * column * scale, textY,
					textHeight, unknownFont, weakColor,
				)
//				context.uiRenderer.drawString(
//					context.resources.font, currentAmount.toString(), weakColor, IntArray(0),
//					partyMinX + 5 * scale + 18 * column * scale, 0, width, height,
//					textY, textHeight, 1, TextAlignment.LEFT
//				)
			}
			indexY += 1
		}
	}

	// Render gold text & icon
	run {
		ovalBatch.simpleAntiAliased(
			goldCircleX - goldRadius, minGoldY,
			goldCircleX + goldRadius, maxGoldY,
			0.1f, goldBackground,
		)
//		context.uiRenderer.fillCircle(
//			goldCircleX - goldRadius, minGoldY,
//			goldCircleX + goldRadius, maxGoldY, goldBackground
//		)
		val goldText = srgbToLinear(rgb(230, 200, 120))
		val goldTextY = maxGoldY - 5 * scale
		val goldTextHeight = 7 * scale
		textBatch.drawString(
			String.format("%,d", context.campaign.gold), region.minX + 27 * scale, goldTextY,
			goldTextHeight, unknownFont, goldText,
		)
//		context.uiRenderer.drawString(
//			context.resources.font, String.format("%,d", context.campaign.gold), goldText,
//			IntArray(0), 27 * scale, minGoldY, goldCircleX, maxGoldY,
//			goldTextY, goldTextHeight, 1, TextAlignment.LEFT
//		)
		textBatch.drawString(
			"+ ${loot.gold}", goldCircleX, goldTextY, goldTextHeight,
			unknownFont, goldText, TextAlignment.RIGHT,
		)
//		context.uiRenderer.drawString(
//			context.resources.font, "+ ${loot.gold}", goldText,
//			IntArray(0), 0, minGoldY, goldCircleX, maxGoldY,
//			goldTextY, goldTextHeight, 1, TextAlignment.RIGHT
//		)
	}

	// Render "Get All" button and "Finish"  button
	run {
		val outlineWidth = 2 * scale / 2
		val textOffsetX = region.minX + 15 * scale
		val textOffsetY = 7 * scale
		val textHeight = 5 * scale
		val getAll = Rectangle(region.minX, region.minY + 26 * scale, 100 * scale, 10 * scale)
		renderButton(
			colorBatch, ovalBatch, textBatch, unknownFont, false, "GET ALL",
			false, loot.selectedElement is BattleLoot.SelectedGetAll, loot.items.isEmpty(),
			getAll, outlineWidth, textOffsetX, region.minY + 26 * scale + textOffsetY, textHeight
		)
		renderButton(
			colorBatch, ovalBatch, textBatch, unknownFont, false, "FINISH",
			false, loot.selectedElement is BattleLoot.SelectedFinish, false,
			Rectangle(region.minX, region.boundY - 35 * scale, 100 * scale, 10 * scale),
			outlineWidth, textOffsetX, region.boundY - 35 * scale + textOffsetY, textHeight
		)
	}

	// Highlight selected party member
	run {
		val borderColor = srgbToLinear(rgb(99, 128, 177))
		val lowColor = srgbToLinear(rgb(19, 65, 114))
		val baseX = partyMinX + 18 * scale * loot.selectedPartyIndex - scale - 1
		val baseY = region.minY + 4 * scale - 1
		gradientWithBorder(
			colorBatch, baseX, baseY, baseX + 18 * scale + 1, baseY + 18 * scale + 1,
			1, 1, borderColor, lowColor, lowColor, 0,
		)
//		context.uiRenderer.fillColor(
//			baseX, baseY, baseX + 18 * scale + 1, baseY + 18 * scale + 1, borderColor,
//			Gradient(1, 1, 18 * scale, 18 * scale, lowColor, lowColor, 0)
//		)
	}

	renderLootInventoryGrid(
		colorBatch, party.map { if (it != null) context.campaign.characterStates[it]!! else null },
		partyMinX + scale, region.boundY - 22 * scale, 18 * scale, scale
	)

	return Pair(colorBatch, textBatch)
}
