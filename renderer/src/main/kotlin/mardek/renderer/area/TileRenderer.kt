package mardek.renderer.area

import mardek.content.area.WaterType
import kotlin.math.max
import kotlin.math.min

internal fun renderTiles(areaContext: AreaRenderContext) {
	areaContext.apply {
		val minTileX = (cameraX - region.width / 2) / tileSize - 1
		val minTileY = (cameraY - region.height / 2) / tileSize - 1
		val maxTileX = 1 + (cameraX + region.width / 2) / tileSize
		val maxTileY = 1 + (cameraY + region.height / 2) / tileSize
		for (tileX in minTileX .. maxTileX) {
			for (tileY in minTileY .. maxTileY) {
				val renderX = tileX * tileSize
				val renderY = tileY * tileSize

				val midTile = state.area.getTile(tileX, tileY + 1)
				if (midTile.sprites.size > 1) {
					val sprite = midTile.sprites[midTile.sprites.size - 2]
					renderJobs.add(SpriteRenderJob(
						x = renderX, y = renderY, sprite = sprite, sortY = renderY + tileSize / 2
					))
				}

				val highTile = state.area.getTile(tileX, tileY + 2)
				if (highTile.sprites.size > 2) {
					val sprite = highTile.sprites[highTile.sprites.size - 3]
					renderJobs.add(SpriteRenderJob(
						x = renderX, y = renderY, sprite = sprite, sortY = renderY + 3 * tileSize / 2
					))
				}
			}
		}

		renderJobs.sort()

		if (state.area.flags.noMovingCamera) {
			val minCameraX = state.area.minTileX * tileSize + region.width / 2 - scissorLeft
			val maxCameraX = (1 + state.area.maxTileX) * tileSize - region.width / 2 + scissorLeft
			if ((1 + state.area.maxTileX) * tileSize > region.width) {
				cameraX = min(maxCameraX, max(minCameraX, cameraX))
			}
			if ((1 + state.area.maxTileY) * tileSize > region.height) {
				cameraY = min(
					(1 + state.area.maxTileY) * tileSize - region.height / 2,
					max(region.height / 2, cameraY)
				)
			}
		}

		for (tileX in minTileX .. maxTileX) {
			for (tileY in minTileY .. maxTileY) {
				val renderX = region.minX + tileX * tileSize + region.width / 2 - cameraX
				val renderY = region.minY + tileY * tileSize + region.height / 2 -cameraY

				val waterType = state.area.getTile(tileX, tileY).waterType
				if (waterType != WaterType.None) {
					var backgroundSprite = state.area.tilesheet.waterSprites[0]
					if (tileY > 0 && state.area.getTile(tileX, tileY - 1).waterType == WaterType.None) {
						backgroundSprite = state.area.tilesheet.waterSprites[1]
					}

					val waterSprite = state.area.tilesheet.waterSprites[waterType.ordinal]

					if (waterType == WaterType.Water) {
						simpleWaterBatch.draw(
							backgroundSprite, waterSprite,
							renderX, renderY, tileX, tileY,
						)
					} else {
						spriteBatch.draw(backgroundSprite, renderX, renderY, scale)
						spriteBatch.draw(waterSprite, renderX, renderY, scale)
					}
				}

				val sprite = state.area.getTile(tileX, tileY).sprites.last()
				spriteBatch.draw(sprite, renderX, renderY, scale)
			}
		}
	}
}
