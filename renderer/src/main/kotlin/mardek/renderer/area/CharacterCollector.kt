package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.area.Direction
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

		val nextPlayerPosition = state.nextPlayerPosition
		for ((index, character) in context.campaign.characterSelection.party.withIndex().reversed()) {
			if (character == null) continue
			val characterState = context.campaign.characterStates[character]!!

			var spriteIndex = 0
			val oldPosition = state.getPlayerPosition(index)

			var x = tileSize * oldPosition.x
			var y = tileSize * oldPosition.y

			if (nextPlayerPosition != null) {
				val newPosition = if (index > 0) state.getPlayerPosition(index - 1)
				else nextPlayerPosition.position

				val p = (state.currentTime - nextPlayerPosition.startTime) / (nextPlayerPosition.arrivalTime - nextPlayerPosition.startTime)
				x = (tileSize * ((1 - p) * oldPosition.x + p * newPosition.x)).roundToInt()
				y = (tileSize * ((1 - p) * oldPosition.y + p * newPosition.y)).roundToInt()

				if (p >= 0.25 && p < 0.75) spriteIndex += 1
			}

			if (index == 0) {
				cameraX = x + tileSize / 2
				cameraY = y + tileSize / 2
			}

			y -= 4 * scale

			val direction = if (index == 0) {
				if (nextPlayerPosition == null) state.lastPlayerDirection else Direction.delta(
					nextPlayerPosition.position.x - oldPosition.x,
					nextPlayerPosition.position.y - oldPosition.y
				)!!
			} else Direction.delta(
				state.getPlayerPosition(index - 1).x - oldPosition.x,
				state.getPlayerPosition(index - 1).y - oldPosition.y
			) ?: Direction.Down

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
