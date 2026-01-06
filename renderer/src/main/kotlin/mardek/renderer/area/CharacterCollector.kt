package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.area.objects.AreaCharacter
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import mardek.state.ingame.area.NextAreaPosition
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun collectAreaCharacters(areaContext: AreaRenderContext) {
	areaContext.apply {
		val animationSize = 2
		val currentTime = state.determineCurrentTime()

		fun collectCharacter(
			character: AreaCharacter, characterState: AreaCharacterState,
			offsetX: Int, opacity: Float, blinkColor: Int, blinkIntensity: Float,
		) {
			val directionalSprites = character.directionalSprites

			val sprite = if (directionalSprites != null) {
				val direction = characterState.direction
				var spriteIndex = animationSize * direction.ordinal
				if (character.walkSpeed == -1) {
					if (currentTime.inWholeMilliseconds % 1000L >= 500L) spriteIndex += 1
				}
				directionalSprites.sprites[spriteIndex]
			} else {
				val fixedSprites = character.fixedSprites!!
				val spriteIndex = (currentTime.inWholeMilliseconds % (200L * fixedSprites.frames.size)) / 200L
				fixedSprites.frames[spriteIndex.toInt()]
			}

			var x = tileSize * characterState.x
			var y = tileSize * characterState.y
			val nextPosition = characterState.next
			if (nextPosition != null) {
				val p = (currentTime - nextPosition.startTime) / (nextPosition.arrivalTime - nextPosition.startTime)
				x = ((1 - p) * x + p * tileSize * nextPosition.position.x).roundToInt()
				y = ((1 - p) * y + p * tileSize * nextPosition.position.y).roundToInt()
			}
			renderJobs.add(SpriteRenderJob(
				x = x + offsetX,
				y = y - 4 * scale,
				sprite = sprite,
				opacity = opacity,
				blinkColor = blinkColor,
				blinkIntensity = blinkIntensity,
			))
		}

		for (character in state.area.objects.characters) {
			val characterState = state.getCharacterState(character) ?: continue
			collectCharacter(character, characterState, 0, 1f, 0, 0f)
		}

		state.fadingCharacters.removeIf { fading ->
			val passedTime = System.nanoTime() - fading.startFadeTime

			// The character should 'vibrate' horizontally:
			// - after 1 flash frame, it should render at state.x + scale
			// - after 2 flash frames, it should render at state.x - scale
			// - after 3 flash frames, it should render at state.x + scale...
			val vibrationPeriod = 66_666_667L // 2 flash frames, in nanoseconds
			val vibrationMod = passedTime % vibrationPeriod
			// vibrationMod = 0 -> offsetX = 1
			// vibrationMod = 0.1 * period -> offsetX = 0.6
			// vibrationMod = 0.2 * period -> offsetX = 0.2
			// vibrationMod = 0.25 * period -> offsetX = 0
			// vibrationMod = 0.5 * period -> offsetX = -1
			// vibrationMod = 0.75 * period -> offsetX = 0
			// vibrationMod ~= period -> offsetX = 1
			val signedVibration = 4f * abs(0.5f - vibrationMod.toFloat() / vibrationPeriod) - 1f
			val offsetX = scale * signedVibration

			// Fading should take 100 flash frames ~= 3.3 seconds
			val redStrength = passedTime / 3_300_000_000f
			if (redStrength < 1f) {
				collectCharacter(
					fading.character, fading.lastState, offsetX.roundToInt(), 1f - redStrength,
					rgb(redStrength, 0f, 0f), redStrength
				)
				false
			} else true
		}

		for ((index, character, characterState) in context.campaign.usedPartyMembers().reversed()) {
			val oldPosition = state.getPlayerPosition(index)
			val direction = state.getPlayerDirection(index)
			val nextPosition = when (val suspension = state.suspension) {
				is AreaSuspensionActions -> suspension.actions.nextPartyPositions[index]
				is AreaSuspensionPlayerWalking -> {
					if (index == 0) {
						suspension.destination
					} else {
						NextAreaPosition(
							state.getPlayerPosition(index - 1),
							suspension.destination.startTime,
							suspension.destination.arrivalTime,
							suspension.destination.transition,
						)
					}
				}
				else -> null
			}

			var spriteIndex = 0

			var x = tileSize * oldPosition.x
			var y = tileSize * oldPosition.y

			if (nextPosition != null) {
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
			val currentNanoTime = System.nanoTime()
			val (blinkColor, blinkIntensity) = if (walkDamage != null && currentNanoTime < walkDamage.time + walkDamageDuration) {
				val intensity = 1f - (currentNanoTime - walkDamage.time).toFloat() / walkDamageDuration
				Pair(srgbToLinear(walkDamage.color), intensity)
			} else Pair(0, 0f)
			renderJobs.add(SpriteRenderJob(
				x = x, y = y, sprite = character.areaSprites.sprites[spriteIndex],
				blinkColor = blinkColor, blinkIntensity = blinkIntensity
			))
		}
	}
}
