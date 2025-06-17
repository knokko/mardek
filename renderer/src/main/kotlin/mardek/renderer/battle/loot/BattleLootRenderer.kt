package mardek.renderer.battle.loot

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.renderer.InGameRenderContext
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import kotlin.math.max

private val referenceTime = System.nanoTime()

class BattleLootRenderer(private val context: InGameRenderContext) {

	private val loot = context.campaign.currentArea!!.battleLoot!!
	private val party = context.campaign.characterSelection.party
	private lateinit var kimBatch: KimBatch

	private val width = context.targetImage.width
	private val height = context.targetImage.height
	private var scale = 1
	private var partyMinX = 0
	private val itemYs = IntArray(loot.items.size + loot.plotItems.size + loot.dreamStones.size)

	fun beforeRendering() {
		this.kimBatch = context.resources.kim1Renderer.startBatch()

		val maxPartyWidth = width / 2
		this.scale = max(1, maxPartyWidth / (18 * party.size))
		this.partyMinX = width - 5 * scale - 18 * scale * party.size

		for ((column, character) in party.withIndex()) {
			if (character == null) continue

			var spriteIndex = 0
			val passedTime = System.nanoTime() - referenceTime
			val animationPeriod = 700_000_000L
			if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1


			kimBatch.requests.add(KimRequest(
				x = partyMinX + column * 18 * scale, y = 5 * scale, scale = scale.toFloat(),
				sprite = character.areaSprites.sprites[spriteIndex], opacity = 1f
			))
		}

		val rowHeight = 18 * scale
		val itemX = width / 10
		var itemY = height / 8
		var row = 0
		for (item in loot.items) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = item.item.sprite, opacity = 1f
			))
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		for (plotItem in loot.plotItems) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = plotItem.sprite, opacity = 1f
			))
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		repeat(loot.dreamStones.size) {
			kimBatch.requests.add(KimRequest(
				x = itemX - 16 * scale, y = itemY, scale = scale.toFloat(),
				sprite = context.content.ui.dreamStone, opacity = 1f
			))
			itemYs[row] = itemY
			row += 1
			itemY += rowHeight
		}
		kimBatch.requests.add(KimRequest(
			x = itemX - 16 * scale, y = height - 20 * scale,
			scale = scale.toFloat(), sprite = context.content.ui.goldIcon, opacity = 1f
		))
	}

	fun render() {
		context.uiRenderer.beginBatch()
		context.uiRenderer.fillColor(
			0, 0, width,
			height, rgba(100, 70, 0, 240)
		)
		run {
			val color = rgb(0, 0, 0)
			val x1 = partyMinX - 20 * scale
			val x2 = partyMinX - 2 * scale
			val y1 = 10 * scale
			val y2 = 25 * scale
			context.uiRenderer.fillColor(0, 0, x1, y1, color)
			context.uiRenderer.fillColorUnaligned(x1, y1, x2, y2, x2, 0, x1, 0, color)
			context.uiRenderer.fillColor(x2, 0, width, y2, color)
		}
		context.uiRenderer.drawString(
			context.resources.font, "Spoils", color, IntArray(0),
			width / 50, 0, width / 3,
		)
		// TODO ehm
		context.uiRenderer.endBatch()
		context.resources.kim1Renderer.submit(kimBatch, context.recorder, context.targetImage)
	}
}
