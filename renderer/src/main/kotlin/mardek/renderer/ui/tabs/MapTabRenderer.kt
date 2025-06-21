package mardek.renderer.ui.tabs

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.content.area.WaterType
import mardek.content.sprite.KimSprite
import mardek.renderer.InGameRenderContext
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.state.title.AbsoluteRectangle
import kotlin.math.min
import kotlin.math.roundToInt

private const val BLINK_PERIOD = 750_000_000L
private val referenceTime = System.nanoTime()

class MapTabRenderer(
	private val context: InGameRenderContext,
	private val region: AbsoluteRectangle,
) : TabRenderer() {

	private val shouldBlink = ((System.nanoTime() - referenceTime) % BLINK_PERIOD) >= BLINK_PERIOD / 2
	private lateinit var kimBatch: KimBatch

	private val scale = min(region.width, region.height) / 70
	private val renderWidth = scale * (context.campaign.currentArea?.area?.width ?: 0)
	private val renderHeight = scale * (context.campaign.currentArea?.area?.height ?: 0)
	private val minX = region.minX + (region.width - renderWidth) / 2
	private val minY = region.minY + (region.height - renderHeight) / 2

	override fun beforeRendering() {
		if (scale <= 0) return
		val area = context.campaign.currentArea?.area ?: return
		if (area.chests.isEmpty() && !shouldBlink) return
		kimBatch = context.resources.kim1Renderer.startBatch()

		if (shouldBlink) {

			fun renderSpriteAtMap(x: Int, y: Int, sprite: KimSprite) {
				if (!area.flags.hasClearMap && !context.campaign.areaDiscovery.readOnly(area).isDiscovered(x, y)) return
				val spriteScale = scale / 16f
				kimBatch.requests.add(KimRequest(
					x = minX + x * scale + scale / 2 - (spriteScale * sprite.width / 2).roundToInt(),
					y = minY + y * scale + scale / 2 - (spriteScale * sprite.height / 2).roundToInt(),
					scale = spriteScale, sprite = sprite
				))
			}
			for (element in area.objects.objects) {
				if (element.conversationName == "c_healingCrystal") {
					renderSpriteAtMap(element.x, element.y, context.content.ui.mapSaveCrystal)
				}
			}
			for (portal in area.objects.portals) {
				val destination = portal.destination.area ?: continue
				if (destination.properties.dreamType != area.properties.dreamType) {
					renderSpriteAtMap(portal.x, portal.y, context.content.ui.mapDreamCircle)
				}
			}
		}
		if (area.chests.isNotEmpty()) kimBatch.requests.add(KimRequest(
			x = region.width * 95 / 100, y = region.minY + region.height / 150,
			scale = region.height / 1200f, sprite = context.content.ui.mapChest
		))
	}

	override fun render() {
		if (scale <= 0) return
		val area = context.campaign.currentArea?.area ?: return

		if (area.chests.isNotEmpty()) {
			val textColor = srgbToLinear(rgb(225, 185, 93))
			val openedChests = area.chests.count { context.campaign.openedChests.contains(it) }
			context.uiRenderer.drawString(
				context.resources.font, "$openedChests/${area.chests.size}", textColor, intArrayOf(),
				region.maxX - region.width / 2, region.minY, region.maxX - region.width / 3, region.maxY,
				region.minY + region.height / 20, region.height / 25, 1, TextAlignment.RIGHT
			)

			val darkLeftColor = srgbToLinear(rgb(217, 181, 87))
			val darkRightColor = srgbToLinear(rgb(166, 114, 27))
			val backgroundColor = srgbToLinear(rgb(64, 42, 31))

			val lightUpColor = srgbToLinear(rgb(243, 229, 181))
			val lightRightColor = srgbToLinear(rgb(206, 147, 107))
			val lightLeftColor = srgbToLinear(rgb(234, 210, 140))

			val barX1 = region.maxX - region.width / 3 + region.width / 50
			val barX2 = barX1 + region.width / 4
			val barMidX = barX1 + ((1 + barX2 - barX1) * (openedChests.toDouble() / area.chests.size)).roundToInt()
			val filledBarWidth = barMidX - barX1

			val barMinY = region.minY + region.height / 70
			val barMaxY = barMinY + region.height / 30

			val barHeight = 1 + barMaxY - barMinY
			val barY1 = barHeight / 5
			val barY2 = barY1 + barHeight / 2

			context.uiRenderer.fillColor(
				barX1, barMinY, barX2, barMaxY, backgroundColor,
				Gradient(0, 0, filledBarWidth, barY1, darkLeftColor, darkRightColor, darkLeftColor),
				Gradient(0, barY1, filledBarWidth, barY2 - barY1, lightLeftColor, lightRightColor, lightUpColor),
				Gradient(0, barY2, filledBarWidth, barHeight - barY2, darkLeftColor, darkRightColor, darkLeftColor),
			)
		}
	}

	override fun postUiRendering() {
		super.postUiRendering()

		if (scale <= 0) return
		val areaState = context.campaign.currentArea ?: return
		val area = areaState.area

		context.resources.colorGridRenderer.startBatch(context.recorder)

		val mapBuffer = context.resources.colorGridRenderer.drawGrid(
			context.recorder, context.targetImage, minX, minY, area.width, area.height + 1, 1, scale
		)

		fun put(x: Int, y: Int, mask: Int) {
			val rawBitIndex = 4 * (x + y * area.width)
			val intIndex = rawBitIndex / 32
			val shift = rawBitIndex % 32
			mapBuffer.put(intIndex, (mapBuffer.get(intIndex) and (15 shl shift).inv()) or (mask shl shift))
		}

		val discovery = context.campaign.areaDiscovery.readOnly(area)
		val renderData = context.resources.areaMap[area.id]!!.data
		for (y in 0 .. area.height) {
			for (x in 0 until area.width) {
				val newMask = if ((discovery.isDiscovered(x, y) || area.flags.hasClearMap) && !area.flags.noMap) {
					val waterType = renderData.getWaterType(x, y)
					when (waterType) {
						WaterType.Water -> 3
						WaterType.Lava -> 4
						else -> if (area.canWalkOnTile(x, y)) 2 else 1
					}
				} else 0
				put(x, y, newMask)
			}
		}

		if (shouldBlink) {
			val playerPosition = areaState.getPlayerPosition(0)
			put(playerPosition.x, playerPosition.y, 5)

			fun putIfDiscovered(x: Int, y: Int, mask: Int) {
				if (discovery.isDiscovered(x, y) || area.flags.hasClearMap) put(x, y, mask)
			}

			// TODO Handle moving characters
			for (character in area.objects.characters) putIfDiscovered(character.startX, character.startY, 6)
			for (element in area.objects.objects) {
				if (element.conversationName == "c_healingCrystal") continue
				putIfDiscovered(element.x, element.y, 6)
			}
			for (portal in area.objects.portals) {
				val destination = portal.destination.area
				if (destination == null || destination.properties.dreamType == area.properties.dreamType) {
					putIfDiscovered(portal.x, portal.y, 6)
				}
			}

			for (chest in area.chests) {
				if (context.campaign.openedChests.contains(chest)) continue
				putIfDiscovered(chest.x, chest.y, 7)
			}

			for (door in area.objects.doors) putIfDiscovered(door.x, door.y, 8)
			for (transition in area.objects.transitions) putIfDiscovered(transition.x, transition.y, 8)
		}

		context.resources.colorGridRenderer.endBatch()

		if (shouldBlink || area.chests.isNotEmpty()) {
			context.resources.kim1Renderer.submit(kimBatch, context.recorder, context.targetImage)
		}
	}
}
