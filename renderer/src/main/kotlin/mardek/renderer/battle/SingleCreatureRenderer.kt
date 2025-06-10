package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animations.Animation
import mardek.content.animations.ColorTransform
import mardek.content.animations.SkeletonPartCastSparkle
import mardek.content.animations.SkeletonPartSkins
import mardek.content.animations.SkeletonPartSwingEffect
import mardek.content.battle.PartyLayoutPosition
import mardek.content.stats.Element
import mardek.state.ingame.battle.BattleMoveBasicAttack
import mardek.state.ingame.battle.BattleMoveSkill
import mardek.state.ingame.battle.BattleSkillTargetSingle
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.ParticleEffectState
import org.joml.Math.toRadians
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FRAME_LENGTH = 33_000_000L

class SingleCreatureRenderer(
	private val context: BattleRenderContext,
	private val combatant: CombatantState,
) {
	private val currentRealTime = System.nanoTime()
	private val flipX = if (combatant.isOnPlayerSide) 1f else -1f
	private val effectColorTransform = mergeColorTransforms(selectedColorTransform(), damageColorTransform())
	private val skeleton = combatant.getModel().skeleton

	private var relativeTime = currentRealTime - context.battle.startTime
	private var animation: Animation? = skeleton.getAnimation("idle")
	private var coordinates = transformBattleCoordinates(
		combatant.getPosition(context.battle), flipX, context.targetImage
	)

	private fun colorCombineTransform(max: Float, intensity: Float, color: Int) = ColorTransform(
		addColor = rgba(
			normalize(red(color)) * max * intensity,
			normalize(green(color)) * max * intensity,
			normalize(blue(color)) * max * intensity, 0f
		),
		multiplyColor = rgb(1f - max * intensity, 1f - max * intensity, 1f - max * intensity)
	)

	private fun selectedColorTransform(intensity: Float) = colorCombineTransform(
		0.5f, intensity, rgb(0f, 0f, 1f)
	)

	private fun selectedColorTransform(): ColorTransform? {
		if (combatant.lastPointedTo == 0L) return null

		val blinkTime = 500_000_000L
		val passedTime = currentRealTime - combatant.lastPointedTo
		if (passedTime >= blinkTime) return null

		return selectedColorTransform(1f - passedTime.toFloat() / blinkTime)
	}

	private fun damageColorTransform(elementColor: Int, intensity: Float) = colorCombineTransform(
		1f, intensity, elementColor
	)

	private fun damageColorTransform(): ColorTransform? {
		val damageIndicator = combatant.lastDamageIndicator
		if (damageIndicator !is DamageIndicatorHealth || damageIndicator.gainedHealth == 0) return null

		val blinkTime = 1000_000_000L
		val passedTime = currentRealTime - damageIndicator.time
		if (passedTime >= blinkTime) return null

		val color = if (damageIndicator.element === context.updateContext.physicalElement) rgb(250, 20, 20)
		else damageIndicator.element.color

		return damageColorTransform(color, 1f - passedTime.toFloat() / blinkTime)
	}

	private fun mergeColorTransforms(base: ColorTransform?, top: ColorTransform?): ColorTransform? {
		if (base == null) return top
		if (top == null) return base

		val addColor = rgba(
			normalize(red(base.addColor)) * normalize(red(top.multiplyColor)) + normalize(red(top.addColor)),
			normalize(green(base.addColor)) * normalize(green(top.multiplyColor)) + normalize(green(top.addColor)),
			normalize(blue(base.addColor)) * normalize(blue(top.multiplyColor)) + normalize(blue(top.addColor)),
			normalize(alpha(base.addColor)) * normalize(alpha(top.multiplyColor)) + normalize(alpha(top.addColor)),
		)
		val multipleColor = rgba(
			normalize(red(base.multiplyColor)) * normalize(red(top.multiplyColor)),
			normalize(green(base.multiplyColor)) * normalize(green(top.multiplyColor)),
			normalize(blue(base.multiplyColor)) * normalize(blue(top.multiplyColor)),
			normalize(alpha(base.multiplyColor)) * normalize(alpha(top.multiplyColor)),
		)
		return ColorTransform(addColor = addColor, multiplyColor = multipleColor)
	}

	fun render() {
		if (context.battle.onTurn === combatant) chooseActiveAnimation() else choosePassiveAnimation()
		renderAnimation()
	}

	private fun chooseMeleeAnimation(
		target: CombatantState, decisionTime: Long,
		setReadyForDamage: () -> Unit, setFinished: () -> Unit
	) {
		relativeTime = currentRealTime - decisionTime

		val moveAnimation = skeleton.getAnimation("moveto")
		val moveTime = moveAnimation.frames.size * FRAME_LENGTH
		animation = moveAnimation

		val rawTargetCoordinates = target.getPosition(context.battle)
		val targetFlipX = if (target.isOnPlayerSide) 1f else -1f
		val targetModel = target.getModel()
		val targetStrikePoint = targetModel.skeleton.strikePoint
		val rawStrikePosition = PartyLayoutPosition(
			rawTargetCoordinates.x - targetStrikePoint.x.roundToInt(),
			rawTargetCoordinates.y + (targetModel.skeleton.groundDistance - skeleton.groundDistance).roundToInt()
		)
		val strikePosition = transformBattleCoordinates(rawStrikePosition, targetFlipX, context.targetImage)

		if (relativeTime >= moveTime) {
			relativeTime -= moveTime

			val strikeAnimation = skeleton.getAnimation("strike")
			val strikeTime = strikeAnimation.frames.size * FRAME_LENGTH
			animation = strikeAnimation

			if (relativeTime >= strikeTime / 2) setReadyForDamage()
			if (relativeTime >= strikeTime) {
				relativeTime -= strikeTime

				val jumpAnimation = skeleton.getAnimation("jumpback")
				val jumpTime = jumpAnimation.frames.size * FRAME_LENGTH
				animation = jumpAnimation

				if (relativeTime >= jumpTime) {
					relativeTime = jumpTime - 1L
					setFinished()
				} else {
					var movementProgress = (relativeTime.toFloat() / jumpTime.toFloat()).pow(1.0f)
					movementProgress = if (movementProgress < 0.2f) 0f
					else (movementProgress - 0.2f) / 0.5f
					if (movementProgress > 1f) movementProgress = 1f
					coordinates.x = (1f - movementProgress) * strikePosition.x + movementProgress * coordinates.x
					coordinates.y = (1f - movementProgress) * strikePosition.y + movementProgress * coordinates.y
				}
			} else {
				coordinates = strikePosition
			}
		} else {
			val movementProgress = relativeTime.toFloat() / moveTime.toFloat()
			coordinates.x = movementProgress * strikePosition.x + (1f - movementProgress) * coordinates.x
			coordinates.y = movementProgress * strikePosition.y + (1f - movementProgress) * coordinates.y
		}
	}

	private fun chooseActiveAnimation() {
		val currentMove = context.battle.currentMove
		if (currentMove is BattleMoveBasicAttack) {
			chooseMeleeAnimation(
				currentMove.target, currentMove.decisionTime,
				{ currentMove.finishedStrike = true },
				{ currentMove.finishedJump = true }
			)
		}
		if (currentMove is BattleMoveSkill) {
			if (currentMove.skill.isMelee) {
				if (currentMove.target !is BattleSkillTargetSingle) throw UnsupportedOperationException(
					"Melee skills like ${currentMove.skill} can only target 1 combatant, but got ${currentMove.target}"
				)
				chooseMeleeAnimation(
					(currentMove.target as BattleSkillTargetSingle).target, currentMove.decisionTime,
					{ currentMove.canProcessDamage = true },
					{ currentMove.finished = true }
				)
			} else {
				val castAnimation = skeleton.getAnimation(currentMove.skill.animation ?: "spellcast")
				val relativeCastTime = currentRealTime - currentMove.decisionTime
				val castTime = castAnimation.frames.size * FRAME_LENGTH
				if (relativeCastTime < castTime) {
					animation = castAnimation
					relativeTime = relativeCastTime
					renderCastShadow(currentMove.skill.element)
				} else currentMove.canProcessDamage = true
			}
		}
	}

	private fun renderCastShadow(element: Element) {
		val rawCoordinates = combatant.getPosition(context.battle)
		val rawShadowPosition = PartyLayoutPosition(
			rawCoordinates.x, rawCoordinates.y + skeleton.groundDistance.roundToInt()
		)
		val shadowPosition = transformBattleCoordinates(rawShadowPosition, flipX, context.targetImage)
		val shadowRadius = 50f

		run {
			val spinTime = 1500_000_000L
			val passedTime = currentRealTime - context.battle.startTime
			val angle = 360f * (passedTime % spinTime) / spinTime
			val corners = arrayOf(225f, 315f, 45f, 135f).map { rawAngle ->
				val finalAngle = toRadians(angle + rawAngle)
				Vector2f(
					shadowPosition.x + shadowRadius * coordinates.scaleX * cos(finalAngle),
					shadowPosition.y + shadowRadius * coordinates.scaleY * sin(finalAngle)
				)
			}.toTypedArray()
			context.resources.partRenderer.render(element.bcSprite, corners, null)
		}

		val background = element.spellCastBackground ?: return
		val aspectRatio = background.height.toFloat() / background.width.toFloat()
		val castRadius = 7 * shadowRadius / 11
		val minX = shadowPosition.x - castRadius * coordinates.scaleX
		val maxX = shadowPosition.x + castRadius * coordinates.scaleX
		val minY = shadowPosition.y - aspectRatio * 2 * castRadius * coordinates.scaleY
		val maxY = shadowPosition.y
		val corners = arrayOf(
			Vector2f(minX, minY), Vector2f(maxX, minY),
			Vector2f(maxX, maxY), Vector2f(minX, maxY)
		)
		context.resources.partRenderer.render(background, corners, null)
	}

	private fun choosePassiveAnimation() {
		val lastDamage = combatant.lastDamageIndicator
		if (combatant.isAlive() && lastDamage != null && lastDamage is DamageIndicatorHealth && lastDamage.gainedHealth < 0) {
			val hurtAnimation = skeleton.getAnimation("hit")
			val sinceDamage = currentRealTime - lastDamage.time
			val hurtFrame = sinceDamage / FRAME_LENGTH
			if (hurtFrame < hurtAnimation.frames.size) {
				animation = hurtAnimation
				relativeTime = sinceDamage
				return
			}
		}

		if (!combatant.isAlive()) {
			if (lastDamage != null) {
				val dieAnimation = skeleton.getAnimation("die")
				val sinceDeath = currentRealTime - lastDamage.time
				val dieFrame = sinceDeath / FRAME_LENGTH
				if (dieFrame < dieAnimation.frames.size) {
					animation = dieAnimation
					relativeTime = sinceDeath
					return
				}
			}

			animation = if (combatant is MonsterCombatantState) null
			else skeleton.getAnimation("dead")
		}
	}

	private fun renderAnimation() {
		val animation = this.animation ?: return
		val animationLength = animation.frames.size * FRAME_LENGTH
		val modTime = relativeTime % animationLength
		val frameIndex = toIntExact(modTime / FRAME_LENGTH)
		val frame = animation.frames[frameIndex]

		for (animationPart in frame.parts) {
			val matrix = animationPart.matrix
			val (scaleX, scaleY) = if (matrix.hasScale) Pair(matrix.scaleX, matrix.scaleY) else Pair(
				1f,
				1f
			)

			val content = animationPart.part.content
			if (content is SkeletonPartSkins) {
				if (content.skins.isEmpty()) continue
				val bodyPart = content.skins.find { it.name == combatant.getModel().skin } ?:
						content.skins.find { it.name == "D_LL" } ?:
						content.skins.find { it.name == "D" } ?:
						content.skins.first()

				if (bodyPart.name == "unknown") continue
				for (entry in bodyPart.entries) {
					val jomlMatrix = Matrix3x2f(
						scaleX * flipX, matrix.rotateSkew0,
						matrix.rotateSkew1 * flipX, scaleY,
						matrix.translateX * flipX, matrix.translateY
					).translate(entry.offsetX, entry.offsetY)

					val corners = arrayOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f), Pair(0f, 1f)).map { rawCorner ->
						val position = jomlMatrix.transformPosition(Vector2f(
							rawCorner.first * entry.sprite.width.toFloat() / entry.scale,
							rawCorner.second * entry.sprite.height.toFloat() / entry.scale
						))

						Vector2f(
							coordinates.x + position.x * coordinates.scaleX,
							coordinates.y + position.y * coordinates.scaleY
						)
					}.toTypedArray()

					val colorTransform = mergeColorTransforms(animationPart.color, effectColorTransform)
					context.resources.partRenderer.render(entry.sprite, corners, colorTransform)
				}
			}
			if (content is SkeletonPartCastSparkle) {
				if (context.battle.onTurn === this.combatant) {
					val currentMove = context.battle.currentMove
					if (currentMove is BattleMoveSkill) {
						val castEffect = currentMove.skill.element.spellCastEffect
						if (castEffect != null && currentRealTime > currentMove.lastCastParticle + 30_000_000L) {
							val basePosition = combatant.getPosition(context.battle)
							val position = PartyLayoutPosition(
								basePosition.x - (flipX * matrix.translateX).toInt(),
								basePosition.y + matrix.translateY.toInt()
							)
							context.battle.particles.add(ParticleEffectState(castEffect, position, combatant.isOnPlayerSide))
							currentMove.lastCastParticle = currentRealTime
						}
					}
				}
			}
			if (content is SkeletonPartSwingEffect) {
				if (context.battle.onTurn === combatant) {
					val currentMove = context.battle.currentMove
					val element = if (currentMove is BattleMoveBasicAttack) {
						val weapon = combatant.getEquipment(context.updateContext)[0]
						weapon?.element ?: context.updateContext.physicalElement
					} else if (currentMove is BattleMoveSkill && currentMove.skill.isMelee) {
						currentMove.skill.element
					} else null

					val sprite = element?.swingEffect
					if (sprite != null) {
						val jomlMatrix = Matrix3x2f(
							scaleX * flipX, matrix.rotateSkew0,
							matrix.rotateSkew1 * flipX, scaleY,
							matrix.translateX * flipX, matrix.translateY
						).translate(-15.35f, -14.50f) // (-15, -14) are the offset of shape 2295

						val corners = arrayOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f), Pair(0f, 1f)).map { rawCorner ->
							val position = jomlMatrix.transformPosition(Vector2f(
								rawCorner.first * sprite.width.toFloat() / 4,
								rawCorner.second * sprite.height.toFloat() / 4
							))

							Vector2f(
								coordinates.x + position.x * coordinates.scaleX,
								coordinates.y + position.y * coordinates.scaleY
							)
						}.toTypedArray()

						val colorTransform = mergeColorTransforms(animationPart.color, effectColorTransform)
						context.resources.partRenderer.render(sprite, corners, colorTransform)
					}
				}
			}
		}
	}
}
