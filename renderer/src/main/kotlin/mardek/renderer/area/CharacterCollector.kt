package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.action.ActionShake
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.FixedActionNode
import mardek.content.area.objects.AreaCharacter
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import mardek.state.ingame.area.NextAreaPosition
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun collectAreaCharacters(areaContext: AreaRenderContext) {
	areaContext.apply {

		fun collectCharacter(
			character: AreaCharacter, characterState: AreaCharacterState,
			offsetX: Int, opacity: Float, blinkColor: Int, blinkIntensity: Float,
		) {
			val directionalSprites = character.directionalSprites

			var x = tileSize * characterState.x
			var y = tileSize * characterState.y
			val nextPosition = characterState.next
			var useAlternativeWalkingSprite = false
			if (nextPosition != null) {
				val p = areaTimings.interpolate(
					nextPosition.startTime, 0f,
					nextPosition.walkDuration, 1f, true,
				)
				x = ((1 - p) * x + p * tileSize * nextPosition.position.x).roundToInt()
				y = ((1 - p) * y + p * tileSize * nextPosition.position.y).roundToInt()
				if (p in 0.25 ..< 0.75) useAlternativeWalkingSprite = true
			}

			val sprite = if (directionalSprites != null) {
				val direction = characterState.direction
				var spriteIndex = direction.baseSpriteIndex
				if (character.walkBehavior.showAnimationWhileStandingStill) {
					spriteIndex = areaTimings.walkingSpriteIndex(direction.baseSpriteIndex)
				} else if (useAlternativeWalkingSprite) spriteIndex += 1
				directionalSprites.sprites[spriteIndex]
			} else {
				val fixedSprites = character.fixedSprites!!
				val spriteIndex = areaTimings.alternateIntegers(
					fixedSprites.frames.size, 200.milliseconds
				)
				fixedSprites.frames[spriteIndex]
			}

			val suspension = state.suspension
			if (suspension is AreaSuspensionActions) {
				val node = suspension.actions.node
				if (node is FixedActionNode) {
					val action = node.action
					if (action is ActionShake) {
						val target = action.target
						if (target is ActionTargetAreaCharacter && target.character === character) {
							val movementsSoFar = areaTimings.elapsedTimeSince(suspension.actions.currentNodeStartTime) / action.stepTime
							val rng = Random(node.id.mostSignificantBits + movementsSoFar.toLong())
							x += scale * rng.nextInt(-action.radius .. action.radius)
							y += scale * rng.nextInt(-action.radius .. action.radius)
						}
					}
				}
			}

			renderJobs.add(SpriteRenderJob(
				x = x + offsetX + scale * character.renderOffsetX,
				y = y - 4 * scale + scale * character.renderOffsetY,
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

			// The character should 'vibrate' horizontally:
			// - after 1 flash frame, it should render at state.x + scale
			// - after 2 flash frames, it should render at state.x - scale
			// - after 3 flash frames, it should render at state.x + scale...
			val offsetX = areaTimings.oscillate(
				-scale.toFloat(), scale.toFloat(), 1.seconds / 15,
				referenceTime = fading.startFadeTime,
			)

			// Fading should take 100 flash frames
			val redStrength = areaTimings.interpolate(
				fading.startFadeTime, 0f,
				100.seconds / 30, 1f, true,
			)
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
							suspension.destination.walkDuration,
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
				val p = areaTimings.interpolate(
					nextPosition.startTime, 0f,
					nextPosition.walkDuration, 1f, true,
				)
				x = (tileSize * ((1 - p) * oldPosition.x + p * nextPosition.position.x)).roundToInt()
				y = (tileSize * ((1 - p) * oldPosition.y + p * nextPosition.position.y)).roundToInt()

				if (p in 0.25 ..< 0.75) spriteIndex += 1
			}

			if (index == 0) {
				cameraX = x + tileSize / 2
				cameraY = y + tileSize / 2
			}

			y -= 4 * scale

			spriteIndex += direction.baseSpriteIndex

			val walkDamage = characterState.lastWalkDamage
			val (blinkColor, blinkIntensity) = if (walkDamage != null) {
				val intensity = context.timing.interpolate(
					walkDamage.time, 1f,
					300.milliseconds, 0f, true
				)
				Pair(srgbToLinear(walkDamage.color), intensity)
			} else Pair(0, 0f)
			renderJobs.add(SpriteRenderJob(
				x = x, y = y, sprite = character.areaSprites.sprites[spriteIndex],
				blinkColor = blinkColor, blinkIntensity = blinkIntensity
			))
		}
	}
}
