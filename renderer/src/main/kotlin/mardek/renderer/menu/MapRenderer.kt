package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.area.AreaTransitionDestination
import mardek.content.area.WaterType
import mardek.content.sprite.BcSprite
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle
import kotlin.math.min
import kotlin.math.roundToInt

private const val BLINK_PERIOD = 750_000_000L
private val UNEXPLORED_COLOR = srgbToLinear(rgb(45, 31, 19))
private val INACCESSIBLE_TERRAIN_COLOR = srgbToLinear(rgb(81, 54, 35))
private val ACCESSIBLE_TERRAIN_COLOR = srgbToLinear(rgb(131, 113, 80))
private val WATER_COLOR = srgbToLinear(rgb(29, 61, 107))
private val LAVA_COLOR = srgbToLinear(rgb(164, 96, 12))
private val PLAYER_COLOR = srgbToLinear(rgb(255, 255, 255))
private val OBJECT_COLOR = srgbToLinear(rgb(0, 255, 0))
private val CLOSED_CHEST_COLOR = srgbToLinear(rgb(255, 221, 119))
private val DOOR_COLOR = srgbToLinear(rgb(0, 255, 255))

internal fun renderAreaMap(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.apply {
		val shouldBlink = ((System.nanoTime() - referenceTime) % BLINK_PERIOD) >= BLINK_PERIOD / 2
		val scale = min(region.width, region.height) / 70

		val areaState = state.state
		if (areaState !is AreaState) return
		val area = areaState.area

		val renderWidth = scale * area.width
		val renderHeight = scale * (1 + area.height)
		val minX = region.minX + (region.width - renderWidth) / 2
		val minY = region.minY + (region.height - renderHeight) / 2

		if (scale <= 0) return

		if (shouldBlink) {

			fun renderSpriteAtMap(x: Int, y: Int, sprite: BcSprite) {
				if (!area.flags.hasClearMap && !context.campaign.areaDiscovery.readOnly(area).isDiscovered(x, y)) return
				val spriteScale = scale / 16f
				val minX = minX + (x - area.minTileX) * scale + scale / 2 - spriteScale * sprite.width / 2
				val minY = minY + (y - area.minTileY) * scale + scale / 2 - spriteScale * sprite.height / 2
				imageBatch.simple(
					minX, minY, minX + (sprite.width * spriteScale).roundToInt() - 1,
					minY + (sprite.height * spriteScale).roundToInt() - 1, sprite.index
				)
			}
			for (element in area.objects.decorations) {
				if (element.conversationName == "c_healingCrystal") {
					renderSpriteAtMap(element.x, element.y, context.content.ui.mapSaveCrystal)
				}
			}
			for (portal in area.objects.portals) {
				val destination = portal.destination
				if (destination is AreaTransitionDestination && destination.area.properties.dreamType != area.properties.dreamType) {
					renderSpriteAtMap(portal.x, portal.y, context.content.ui.mapDreamCircle)
				}
			}
		}

		if (area.chests.isNotEmpty()) {
			spriteBatch.simple(
				region.width * 95 / 100, region.minY + region.height / 150,
				region.height / 1200f, context.content.ui.mapChest.index
			)
			val textColor = srgbToLinear(rgb(225, 185, 93))
			val openedChests = area.chests.count { context.campaign.openedChests.contains(it) }
			textBatch.drawString(
				"$openedChests/${area.chests.size}", region.maxX - region.width / 3,
				region.minY + region.height / 20, region.height / 25,
				context.bundle.getFont(context.content.fonts.basic2.index),
				textColor, TextAlignment.RIGHT
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

			colorBatch.fill(barX1 + filledBarWidth, barMinY, barX2, barMaxY, backgroundColor)
			if (filledBarWidth > 0) {
				colorBatch.gradient(
					barX1, barMinY, barX1 + filledBarWidth, barMinY + barY1,
					darkLeftColor, darkRightColor, darkLeftColor
				)
				colorBatch.gradient(
					barX1, barMinY + barY1, barX1 + filledBarWidth, barMinY + barY2,
					lightLeftColor, lightRightColor, lightUpColor
				)
				colorBatch.gradient(
					barX1, barMinY + barY2, barX1 + filledBarWidth, barMaxY,
					darkLeftColor, darkRightColor, darkLeftColor
				)
			}
		}

		fun putMapColor(tileX: Int, tileY: Int, color: Int) {
			val slotX = minX + (tileX - area.minTileX) * scale
			val slotY = minY + (tileY - area.minTileY) * scale
			colorBatch.fill(slotX, slotY, slotX + scale - 1, slotY + scale - 1, color)
		}
		val discovery = context.campaign.areaDiscovery.readOnly(area)
		for (y in area.minTileY .. 1 + area.maxTileY) {
			for (x in area.minTileX .. area.maxTileX) {
				val color = if ((discovery.isDiscovered(x, y) || area.flags.hasClearMap) && !area.flags.noMap) {
					when (area.getTile(x, y).waterType) {
						WaterType.Water -> WATER_COLOR
						WaterType.Lava -> LAVA_COLOR
						else -> if (area.canWalkOnTile(x, y)) ACCESSIBLE_TERRAIN_COLOR else INACCESSIBLE_TERRAIN_COLOR
					}
				} else UNEXPLORED_COLOR
				putMapColor(x, y, color)
			}
		}

		if (shouldBlink) {
			val playerPosition = areaState.getPlayerPosition(0)
			putMapColor(playerPosition.x, playerPosition.y, PLAYER_COLOR)

			fun putIfDiscovered(x: Int, y: Int, color: Int) {
				if (discovery.isDiscovered(x, y) || area.flags.hasClearMap) putMapColor(x, y, color)
			}

			// TODO CHAP1 Handle moving characters
			for (character in area.objects.characters) {
				putIfDiscovered(character.startX, character.startY, OBJECT_COLOR)
			}
			for (element in area.objects.decorations) {
				if (element.conversationName == "c_healingCrystal" || element.canWalkThrough) continue
				putIfDiscovered(element.x, element.y, OBJECT_COLOR)
			}
			for (portal in area.objects.portals) {
				val destination = portal.destination
				if (destination !is AreaTransitionDestination || destination.area.properties.dreamType == area.properties.dreamType) {
					putIfDiscovered(portal.x, portal.y, OBJECT_COLOR)
				}
			}

			for (chest in area.chests) {
				if (context.campaign.openedChests.contains(chest)) continue
				putIfDiscovered(chest.x, chest.y, CLOSED_CHEST_COLOR)
			}

			for (door in area.objects.doors) putIfDiscovered(door.x, door.y, DOOR_COLOR)
			for (transition in area.objects.transitions) putIfDiscovered(transition.x, transition.y, DOOR_COLOR)
		}

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		textBatch.drawString(
			area.properties.displayName, region.minX + 0.02f * region.height,
			region.maxY + 0.075f * region.height, 0.04f * region.height,
			font, srgbToLinear(rgb(238, 203, 127)),
		)
	}
}
