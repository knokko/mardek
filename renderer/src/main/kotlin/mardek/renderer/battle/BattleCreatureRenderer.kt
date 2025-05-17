package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animations.BattleModel
import mardek.content.animations.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.state.ingame.battle.BattleMoveBasicAttack
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.MonsterCombatantState
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact
import kotlin.math.pow
import kotlin.math.roundToInt

class BattleCreatureRenderer(
	private val context: BattleRenderContext,
) {
	private val currentRealTime = System.nanoTime()

	private fun selectedColorTransform(intensity: Float) = ColorTransform(
		addColor = rgba(0f, 0f, 0.5f * intensity, 0f),
		multiplyColor = rgb(1f - 0.5f * intensity, 1f - 0.5f * intensity, 1f - 0.5f * intensity)
	)

	private fun selectedColorTransform(state: CombatantState): ColorTransform? {
		if (state.lastPointedTo == 0L) return null

		val blinkTime = 500_000_000L
		val passedTime = System.nanoTime() - state.lastPointedTo
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
		context.resources.partRenderer.startBatch(context.recorder)
		context.resources.partRenderer.render(context.battle.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for (enemy in context.battle.allOpponents()) renderCreature(enemy)
		for (player in context.battle.allPlayers()) renderCreature(player)

		context.resources.partRenderer.endBatch()
	}

	private fun renderCreature(combatant: CombatantState) {
		// TODO Monster death animation
		if (combatant is MonsterCombatantState && !combatant.isAlive()) return

		val (rawPosition, flipX) = getRawCoordinates(combatant)
		var coordinates = transformBattleCoordinates(rawPosition, flipX, context.targetImage)
		val effectColorTransform = selectedColorTransform(combatant)
		val model = combatant.getModel()

		var relativeTime = currentRealTime - context.battle.startTime
		var animation = model.skeleton.getAnimation("idle")
		val frameLength = 33_000_000L

		val currentMove = context.battle.currentMove
		val onTurn = context.battle.onTurn
		if (onTurn == combatant) {
			// die, hit, idle, spellcast, strike, dead, jumpback, useitem, moveto
			if (currentMove is BattleMoveBasicAttack) {
				relativeTime = currentRealTime - currentMove.decisionTime

				val moveAnimation = model.skeleton.getAnimation("moveto")
				val moveTime = moveAnimation.frames.size * frameLength
				animation = moveAnimation

				val (rawTargetCoordinates, targetFlipX) = getRawCoordinates(currentMove.target)
				val targetModel = currentMove.target.getModel()
				val targetHitPoint = targetModel.skeleton.hitPoint
				val myStrikePoint = model.skeleton.strikePoint
				val targetStrikePoint = targetModel.skeleton.strikePoint
				val rawStrikePosition = PartyLayoutPosition(
					//rawTargetCoordinates.x - (0.05f * (myStrikePoint.x - targetHitPoint.x)).roundToInt(),
					rawTargetCoordinates.x - (0.05f * (targetStrikePoint.x - myStrikePoint.x)).roundToInt(),
					rawTargetCoordinates.y - (0.05f * (targetStrikePoint.y - myStrikePoint.y)).roundToInt()
				)
				val strikePosition = transformBattleCoordinates(rawStrikePosition, targetFlipX, context.targetImage)

				if (relativeTime >= moveTime) {
					relativeTime -= moveTime

					val strikeAnimation = model.skeleton.getAnimation("strike")
					val strikeTime = strikeAnimation.frames.size * frameLength
					animation = strikeAnimation

					if (relativeTime >= strikeTime / 2) currentMove.finishedStrike = true
					if (relativeTime >= strikeTime) {
						relativeTime -= strikeTime

						val jumpAnimation = model.skeleton.getAnimation("jumpback")
						val jumpTime = jumpAnimation.frames.size * frameLength
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

		val animationLength = animation.frames.size * frameLength
		val modTime = relativeTime % animationLength
		val frameIndex = toIntExact(modTime / frameLength)
		val frame = animation.frames[frameIndex]
		for (animationPart in frame.parts) {
			val matrix = animationPart.matrix
			val (scaleX, scaleY) = if (matrix.hasScale) Pair(matrix.scaleX, matrix.scaleY) else Pair(
				1f,
				1f
			)

			if (animationPart.part.skins.isEmpty()) continue
			val bodyPart = animationPart.part.skins.find { it.name == model.skin } ?:
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

	private fun getRawCoordinates(combatant: CombatantState): Pair<PartyLayoutPosition, Float> {
		val partyLayout = if (combatant.isOnPlayerSide) context.battle.playerLayout else context.battle.battle.enemyLayout
		val index = if (combatant.isOnPlayerSide) context.battle.players.indexOf(combatant)
		else context.battle.opponents.indexOf(combatant)
		val rawPosition = partyLayout.positions[index]
		val flipX = if (combatant.isOnPlayerSide) 1f else -1f
		return Pair(rawPosition, flipX)
	}
}
