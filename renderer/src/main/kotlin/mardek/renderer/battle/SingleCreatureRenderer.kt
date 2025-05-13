package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animations.Animation
import mardek.content.animations.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.state.ingame.battle.BattleMoveBasicAttack
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.ingame.battle.MonsterCombatantState
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact
import kotlin.math.pow
import kotlin.math.roundToInt

private const val FRAME_LENGTH = 33_000_000L

class SingleCreatureRenderer(
	private val context: BattleRenderContext,
	private val combatant: CombatantState,
) {
	private val currentRealTime = System.nanoTime()
	private val flipX = if (combatant.isOnPlayerSide) 1f else -1f
	private val effectColorTransform = selectedColorTransform()
	private val skeleton = combatant.getModel().skeleton

	private var relativeTime = currentRealTime - context.battle.startTime
	private var animation: Animation? = skeleton.getAnimation("idle")
	private var coordinates = transformBattleCoordinates(
		combatant.getPosition(context.battle), flipX, context.targetImage
	)

	private fun selectedColorTransform(intensity: Float) = ColorTransform(
		addColor = rgba(0f, 0f, 0.5f * intensity, 0f),
		multiplyColor = rgb(1f - 0.5f * intensity, 1f - 0.5f * intensity, 1f - 0.5f * intensity)
	)

	private fun selectedColorTransform(): ColorTransform? {
		if (combatant.lastPointedTo == 0L) return null

		val blinkTime = 500_000_000L
		val passedTime = currentRealTime - combatant.lastPointedTo
		if (passedTime >= blinkTime) return null

		return selectedColorTransform(1f - passedTime.toFloat() / blinkTime)
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

	private fun chooseActiveAnimation() {
		// die, hit, idle, spellcast, strike, dead, jumpback, useitem, moveto
		val currentMove = context.battle.currentMove
		if (currentMove is BattleMoveBasicAttack) {
			relativeTime = currentRealTime - currentMove.decisionTime

			val moveAnimation = skeleton.getAnimation("moveto")
			val moveTime = moveAnimation.frames.size * FRAME_LENGTH
			animation = moveAnimation

			val rawTargetCoordinates = currentMove.target.getPosition(context.battle)
			val targetFlipX = if (currentMove.target.isOnPlayerSide) 1f else -1f
			val targetModel = currentMove.target.getModel()
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

				if (relativeTime >= strikeTime / 2) currentMove.finishedStrike = true
				if (relativeTime >= strikeTime) {
					relativeTime -= strikeTime

					val jumpAnimation = skeleton.getAnimation("jumpback")
					val jumpTime = jumpAnimation.frames.size * FRAME_LENGTH
					animation = jumpAnimation

					if (relativeTime >= jumpTime) {
						relativeTime = jumpTime - 1L
						currentMove.finishedJump = true
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
			else skeleton.getAnimation("death")
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

			if (animationPart.part.skins.isEmpty()) continue
			val bodyPart = animationPart.part.skins.find { it.name == combatant.getModel().skin } ?:
			animationPart.part.skins.find { it.name == "D_LL" } ?:
			animationPart.part.skins.find { it.name == "D" } ?:
			animationPart.part.skins.first()

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
	}
}
