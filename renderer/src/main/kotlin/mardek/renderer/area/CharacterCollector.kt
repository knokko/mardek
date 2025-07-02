package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.area.Direction
import mardek.state.ingame.area.NextAreaPosition
import kotlin.math.roundToInt

internal fun collectAreaCharacters(areaContext: AreaRenderContext) {
	areaContext.apply {
		val animationSize = 2

		for (character in state.area.objects.characters) {
			val direction = character.startDirection ?: Direction.Down
			var spriteIndex = animationSize * direction.ordinal
			if (character.walkSpeed == -1) {
				if (state.currentTime.inWholeMilliseconds % 1000L >= 500L) spriteIndex += 1
			}
			renderJobs.add(SpriteRenderJob(
				x = tileSize * character.startX,
				y = tileSize * character.startY - 4 * scale,
				sprite = character.sprites.sprites[spriteIndex]
			))
		}

		val actionState = state.actions
		for ((index, character) in context.campaign.characterSelection.party.withIndex().reversed()) {
			if (character == null) continue
			val characterState = context.campaign.characterStates[character]!!

			val oldPosition = state.getPlayerPosition(index)
			val direction = state.getPlayerDirection(index)
			val nextPosition = if (actionState == null) {
				val nextPlayerPosition = state.nextPlayerPosition
				if (index == 0) {
					nextPlayerPosition
				} else {
					if (nextPlayerPosition != null) {
						NextAreaPosition(
							state.getPlayerPosition(index - 1),
							nextPlayerPosition.startTime,
							nextPlayerPosition.arrivalTime,
						)
					} else null
				}
			} else {
				actionState.nextPartyPositions[index]
			}

			var spriteIndex = 0

			var x = tileSize * oldPosition.x
			var y = tileSize * oldPosition.y

			if (nextPosition != null) {
				val currentTime = actionState?.currentTime ?: state.currentTime
				val p = (currentTime - nextPosition.startTime) / (nextPosition.arrivalTime - nextPosition.startTime)
				x = (tileSize * ((1 - p) * oldPosition.x + p * nextPosition.position.x)).roundToInt()
				y = (tileSize * ((1 - p) * oldPosition.y + p * nextPosition.position.y)).roundToInt()

				if (p in 0.25 ..< 0.75) spriteIndex += 1
			}

			if (index == 0) {
				cameraX = x + tileSize / 2
				cameraY = y + tileSize / 2
			}

			y -= 4 * scale

			spriteIndex += animationSize * direction.ordinal

			val walkDamage = characterState.lastWalkDamage
			val walkDamageDuration = 500_000_000L
			val currentTime = System.nanoTime()
			val (blinkColor, blinkIntensity) = if (walkDamage != null && currentTime < walkDamage.time + walkDamageDuration) {
				val intensity = 1f - (currentTime - walkDamage.time).toFloat() / walkDamageDuration
				Pair(srgbToLinear(walkDamage.color), intensity)
			} else Pair(0, 0f)
			renderJobs.add(SpriteRenderJob(
				x = x, y = y, sprite = character.areaSprites.sprites[spriteIndex],
				blinkColor = blinkColor, blinkIntensity = blinkIntensity
			))
		}
	}
}
