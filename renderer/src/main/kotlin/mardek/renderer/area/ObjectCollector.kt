package mardek.renderer.area

import mardek.content.area.AreaDreamType
import mardek.state.ingame.area.AreaState
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

internal fun collectAreaObjects(areaContext: AreaRenderContext) {
	areaContext.apply {
		for (chest in state.area.chests) {
			if (chest.hidden) continue
			val sprite = if (context.campaign.openedChests.contains(chest)) {
				chest.sprite.openedSprite
			} else chest.sprite.baseSprite

			renderJobs.add(SpriteRenderJob(
				x = tileSize * chest.x,
				y = tileSize * chest.y,
				sprite = sprite,
			))
		}

		for (decoration in state.area.objects.decorations) {
			val spritesheet = decoration.sprites ?: continue
			val spriteIndex = (state.currentTime.inWholeMilliseconds % (decoration.timePerFrame * spritesheet.frames.size)) / decoration.timePerFrame

			val sprite = spritesheet.frames[spriteIndex.toInt()]
			renderJobs.add(SpriteRenderJob(
				x = tileSize * decoration.x,
				y = tileSize * decoration.y,
				sprite = sprite
			))
		}

		for (door in state.area.objects.doors) {
			var spriteIndex = 0
			val openingDoor = state.openingDoor

			if (openingDoor != null && door == openingDoor.door) {
				val startTime = openingDoor.finishTime - AreaState.DOOR_OPEN_DURATION
				val progress = (state.currentTime - startTime) / AreaState.DOOR_OPEN_DURATION
				spriteIndex = min((door.sprites.frames.size * progress).toInt(), door.sprites.frames.size - 1)
			}
			renderJobs.add(SpriteRenderJob(
				x = tileSize * door.x,
				y = tileSize * door.y,
				sprite = door.sprites.frames[spriteIndex]
			))
		}

		for (areaObject in state.area.objects.objects) {
			val spriteIndex = (state.currentTime.inWholeMilliseconds % (200L * areaObject.sprites.frames.size)) / 200L

			renderJobs.add(SpriteRenderJob(
				x = tileSize * areaObject.x,
				y = tileSize * areaObject.y - 4 * scale,
				sprite = areaObject.sprites.frames[spriteIndex.toInt()]
			))
		}

		for (portal in state.area.objects.portals) {
			val spritesheet = portal.sprites ?: continue

			val isDream = state.area.properties.dreamType != AreaDreamType.None
			val destinationArea = portal.destination.area

			// When exactly 1 of the current area and destination is a dream area, the portal must be a dream circle
			// Hence we should not render the portal texture
			if (destinationArea != null && isDream != (destinationArea.properties.dreamType != AreaDreamType.None)) {
				continue
			}

			val spriteIndex = (state.currentTime.inWholeMilliseconds % (15000L * spritesheet.frames.size)) / 15000L
			renderJobs.add(SpriteRenderJob(
				x = tileSize * portal.x,
				y = tileSize * portal.y,
				sprite = spritesheet.frames[spriteIndex.toInt()]
			))
		}

		for (gate in state.area.objects.switchGates) {
			renderJobs.add(SpriteRenderJob(
				x = tileSize * gate.x,
				y = tileSize * gate.y,
				sprite = gate.color.gateSprite
			))
		}

		for (orb in state.area.objects.switchOrbs) {
			renderJobs.add(SpriteRenderJob(
				x = tileSize * orb.x,
				y = tileSize * orb.y - 4 * scale,
				sprite = orb.color.onSprite
			))
		}

		for (platform in state.area.objects.switchPlatforms) {
			renderJobs.add(SpriteRenderJob(
				x = tileSize * platform.x,
				y = tileSize * platform.y,
				sprite = platform.color.platformSprite
			))
		}

		for (transition in state.area.objects.transitions) {
			val arrow = transition.arrow ?: continue

			val period = 1000
			val relativeTime = state.currentTime.inWholeMilliseconds % period
			val opacity = 0.5 + 0.5 * sin(2 * PI * relativeTime / period)
			renderJobs.add(SpriteRenderJob(
				x = tileSize * transition.x,
				y = tileSize * transition.y,
				sprite = arrow.sprite,
				opacity = opacity.toFloat()
			))
		}
	}
}
