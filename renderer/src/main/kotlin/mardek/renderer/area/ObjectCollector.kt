package mardek.renderer.area

import mardek.content.area.AreaDreamType
import mardek.content.area.AreaTransitionDestination
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionOpeningDoor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun collectAreaObjects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
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
			val spriteIndex = areaTimings.alternateIntegers(
				spritesheet.frames.size, decoration.timePerFrame
			)

			val sprite = spritesheet.frames[spriteIndex]
			var y = tileSize * decoration.y
			if (!decoration.canWalkThrough) y -= 4 * scale // TODO CHAP2 Find less dirty way to deal with this

			renderJobs.add(SpriteRenderJob(x = tileSize * decoration.x, y = y, sprite = sprite))
		}

		for (door in state.area.objects.doors) {
			var spriteIndex = 0
			val openingDoor = suspension as? AreaSuspensionOpeningDoor

			if (openingDoor != null && door == openingDoor.door) {
				spriteIndex = areaTimings.interpolate(
					openingDoor.startTime, 0,
					AreaState.DOOR_OPEN_DURATION, door.sprites.frames.size - 1, true,
				)
			}
			renderJobs.add(SpriteRenderJob(
				x = tileSize * door.x,
				y = tileSize * door.y,
				sprite = door.sprites.frames[spriteIndex]
			))
		}

		for (portal in state.area.objects.portals) {
			val spritesheet = portal.sprites ?: continue

			val isDream = state.area.properties.dreamType != AreaDreamType.None
			val destination = portal.destination

			// When exactly 1 of the current area and destination is a dream area, the portal must be a dream circle
			// Hence we should not render the portal texture
			if (destination is AreaTransitionDestination && isDream != (destination.area.properties.dreamType != AreaDreamType.None)) {
				continue
			}

			val spriteIndex = areaTimings.alternateIntegers(
				spritesheet.frames.size - 1, 15.milliseconds
			)
			renderJobs.add(SpriteRenderJob(
				x = tileSize * portal.x,
				y = tileSize * portal.y,
				sprite = spritesheet.frames[spriteIndex]
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
			renderJobs.add(SpriteRenderJob(
				x = tileSize * transition.x,
				y = tileSize * transition.y,
				sprite = arrow.sprite,
				opacity = areaTimings.oscillate(0f, 1f, 1.seconds)
			))
		}
	}
}
