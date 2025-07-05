package mardek.renderer.battle.loot

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.InGameRenderContext
import mardek.renderer.area.renderLootInventoryGrid
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.changeAlpha
import mardek.renderer.ui.renderButton
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max

private val referenceTime = System.nanoTime()

class BattleLootRenderer(private val context: InGameRenderContext) {

	private val loot = context.campaign.currentArea!!.battleLoot!!
	private val party = context.campaign.characterSelection.party
	private lateinit var kimBatch: KimBatch

	private val width = context.viewportWidth
	private val height = context.viewportHeight
	private var scale = 1
	private var partyMinX = 0
	private val itemYs = IntArray(loot.items.size + loot.plotItems.size + loot.dreamStones.size)

	fun beforeRendering() {
		this.kimBatch = context.resources.kim1Renderer.startBatch()

		this.scale = max(1, height / (11 * 16))
		this.partyMinX = width - 5 * scale - 18 * scale * party.size

		for ((column, character) in party.withIndex()) {
			if (character == null) continue

			var spriteIndex = 0
			val passedTime = System.nanoTime() - referenceTime
			val animationPeriod = 700_000_000L
			if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

			kimBatch.requests.add(KimRequest(
				x = partyMinX + column * 18 * scale, y = 5 * scale, scale = scale.toFloat(),
				sprite = character.areaSprites.sprites[spriteIndex]
			))
		}

		val selectedElement = loot.selectedElement
		val pointer = context.content.ui.horizontalPointer
		val pointerScale = 6f * scale / pointer.height
		val rowHeight = 18 * scale
		val itemX = 30 * scale
		var itemY = 40 * scale
		var row = 0
		for (item in loot.items) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = item.item.sprite
			))
			if (selectedElement is BattleLoot.SelectedItem && row == selectedElement.index) {
				kimBatch.requests.add(KimRequest(
					x = itemX - 25 * scale, y = itemY + 5 * scale,
					scale = pointerScale, sprite = pointer
				))
			}
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		if (selectedElement is BattleLoot.SelectedGetAll) {
			kimBatch.requests.add(KimRequest(x = 3 * scale, y = 28 * scale, pointerScale, sprite = pointer))
		}
		if (selectedElement is BattleLoot.SelectedFinish) {
			kimBatch.requests.add(KimRequest(x = 3 * scale, y = height - 33 * scale, pointerScale, sprite = pointer))
		}
		for (plotItem in loot.plotItems) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = plotItem.sprite
			))
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		repeat(loot.dreamStones.size) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = context.content.ui.dreamStoneIcon
			))
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		kimBatch.requests.add(KimRequest(
			x = 8 * scale, y = height - 20 * scale,
			scale = scale.toFloat(), sprite = context.content.ui.goldIcon
		))
	}

	fun render() {
		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context, 11)

		val leftColor = srgbToLinear(rgba(54, 37, 21, 240))
		val rightColor = srgbToLinear(rgba(132, 84, 53, 240))
		val x1 = partyMinX - 20 * scale
		val x2 = partyMinX - 2 * scale
		val y1 = 12 * scale
		val y2 = 24 * scale
		rectangles.gradient(
			0, 0, width - 1, height - 1,
			leftColor, rightColor, leftColor
		)

		// Render dark top bar & bottom bar
		val upColor = changeAlpha(rightColor, 100)
		rectangles.fill(0, y1, width - 1, y2, upColor)
		val barColor = rgb(0, 0, 0)
		rectangles.fill(0, 0, x1, y1, barColor)
		rectangles.fillUnaligned(x1, y1, x2, y2, x2, 0, x1, 0, barColor)
		rectangles.fill(x2, 0, width, y2, barColor)

		val x4 = width - 55 * scale
		val x3 = x4 - 4 * scale
		val y4 = height - 25 * scale
		val y3 = y4 - 5 * scale
		rectangles.fill(0, y4, width - 1, height - 1, barColor)
		rectangles.fillUnaligned(x3, y4, width, y4, width, y3, x4, y3, barColor)

		val selectedElement = loot.selectedElement
		for (indexY in loot.items.indices) {
			if (selectedElement is BattleLoot.SelectedItem && selectedElement.index == indexY) {
				val color = srgbToLinear(rgb(51, 102, 204))
				val itemY = itemYs[indexY]
				rectangles.fill(
					0, itemY, width - 1, itemY + 16 * scale,
					changeAlpha(color, 50)
				)
				rectangles.fill(0, itemY, width - 1, itemY + 1, color)
				rectangles.fill(
					0, itemY + 16 * scale - 1,
					width - 1, itemY + 16 * scale, color
				)
			}
		}

		val goldBackground = srgbToLinear(rgb(35, 23, 15))
		val minGoldY = height - 21 * scale
		val maxGoldY = height - 3 * scale
		val goldCircleX = width / 2
		val goldRadius = (maxGoldY - minGoldY) / 2
		rectangles.fill(0, minGoldY, goldCircleX, maxGoldY, goldBackground)

		rectangles.endBatch(context.recorder)

		context.uiRenderer.beginBatch()

		context.uiRenderer.drawString(
			context.resources.font, "INVENTORY SPACE", srgbToLinear(rgb(131, 81, 37)),
			IntArray(0), x4, y3, width - 1, height - 1,
			y4, 4 * scale, 1, TextAlignment.LEFT
		)

		// Render "Spoils" and the random title
		context.uiRenderer.drawString(
			context.resources.font, "Spoils", srgbToLinear(rgb(131, 81, 37)),
			IntArray(0), width / 50, 0, width / 3, height / 3,
			9 * scale, 6 * scale, 1, TextAlignment.LEFT
		)
		context.uiRenderer.drawString(
			context.resources.font, loot.itemText, srgbToLinear(rgb(207, 192, 141)),
			IntArray(0), width / 50, 0, width / 2, height / 3,
			20 * scale, 4 * scale, 1, TextAlignment.LEFT
		)

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
				context.uiRenderer.drawString(
					context.resources.font, itemStack.item.flashName, strongColor,
					IntArray(0), 32 * scale, 0, width / 2, height,
					textY, textHeight, 1, TextAlignment.LEFT
				)
				context.uiRenderer.drawString(
					context.resources.font, "x ${itemStack.amount}", strongColor,
					IntArray(0), 140 * scale, 0, width, height,
					textY, textHeight, 1, TextAlignment.LEFT
				)
				for ((column, member) in party.withIndex()) {
					if (member == null) continue
					val currentAmount = context.campaign.characterStates[member]!!.countItemOccurrences(itemStack.item)
					context.uiRenderer.drawString(
						context.resources.font, currentAmount.toString(), weakColor, IntArray(0),
						partyMinX + 5 * scale + 18 * column * scale, 0, width, height,
						textY, textHeight, 1, TextAlignment.LEFT
					)
				}
				indexY += 1
			}
		}

		// Render gold text & icon
		run {
			context.uiRenderer.fillCircle(
				goldCircleX - goldRadius, minGoldY,
				goldCircleX + goldRadius, maxGoldY, goldBackground
			)
			val goldText = srgbToLinear(rgb(230, 200, 120))
			val goldTextY = maxGoldY - 5 * scale
			val goldTextHeight = 7 * scale
			context.uiRenderer.drawString(
				context.resources.font, String.format("%,d", context.campaign.gold), goldText,
				IntArray(0), 27 * scale, minGoldY, goldCircleX, maxGoldY,
				goldTextY, goldTextHeight, 1, TextAlignment.LEFT
			)
			context.uiRenderer.drawString(
				context.resources.font, "+ ${loot.gold}", goldText,
				IntArray(0), 0, minGoldY, goldCircleX, maxGoldY,
				goldTextY, goldTextHeight, 1, TextAlignment.RIGHT
			)
		}

		// Render "Get All" button and "Finish"  button
		run {
			val outlineWidth = 3 * scale / 2
			val textOffsetX = 15 * scale
			val textOffsetY = 7 * scale
			val textHeight = 5 * scale
			val getAll = AbsoluteRectangle(0, 26 * scale, 100 * scale, 10 * scale)
			renderButton(
				context.uiRenderer, context.resources.font, false, "GET ALL",
				loot.selectedElement is BattleLoot.SelectedGetAll, getAll, outlineWidth, textOffsetX,
				26 * scale + textOffsetY, textHeight
			)
			if (loot.items.isEmpty()) {
				context.uiRenderer.fillColor(
					getAll.minX, getAll.minY, getAll.maxX, getAll.maxY,
					srgbToLinear(rgba(54, 37, 21, 200))
				)
			}
			renderButton(
				context.uiRenderer, context.resources.font, false, "FINISH",
				loot.selectedElement is BattleLoot.SelectedFinish,
				AbsoluteRectangle(0, height - 35 * scale, 100 * scale, 10 * scale),
				outlineWidth, textOffsetX, height - 35 * scale + textOffsetY, textHeight
			)
		}

		// Highlight selected party member
		run {
			val borderColor = srgbToLinear(rgb(99, 128, 177))
			val lowColor = srgbToLinear(rgb(19, 65, 114))
			val baseX = partyMinX + 18 * scale * loot.selectedPartyIndex - scale - 1
			val baseY = 4 * scale - 1
			context.uiRenderer.fillColor(
				baseX, baseY, baseX + 18 * scale + 1, baseY + 18 * scale + 1, borderColor,
				Gradient(1, 1, 18 * scale, 18 * scale, lowColor, lowColor, 0)
			)
		}
		context.uiRenderer.endBatch()
		context.resources.kim1Renderer.submit(kimBatch, context)

		renderLootInventoryGrid(
			context, party.map { if (it != null) context.campaign.characterStates[it]!! else null },
			partyMinX + scale, height - 22 * scale, 18 * scale, scale
		)
	}
}
