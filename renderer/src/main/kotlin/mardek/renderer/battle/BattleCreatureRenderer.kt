package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.content.animations.BattleModel
import mardek.content.animations.ColorTransform
import mardek.content.battle.PartyLayoutPosition
import mardek.state.ingame.battle.BattleMoveBasicAttack
import mardek.state.ingame.battle.CombatantReference
import mardek.state.ingame.battle.CombatantState
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact
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

		for (enemy in context.battle.livingEnemies()) renderCreature(enemy)

		for (player in context.battle.allPlayers()) renderCreature(player)

		context.resources.partRenderer.endBatch()
	}

	private fun renderCreature(combatant: CombatantReference) {
		val (rawPosition, flipX) = getRawCoordinates(combatant)
		var coordinates = transformBattleCoordinates(rawPosition, flipX, context.targetImage)
		val effectColorTransform = selectedColorTransform(combatant.getState())
		val model = getModel(combatant)

		var relativeTime = currentRealTime - context.battle.startTime
		var animation = model.skeleton.getAnimation("idle")
		val frameLength = 33_000_000L

		val currentMove = context.battle.currentMove
		val onTurn = context.battle.onTurn
		if (onTurn == combatant) {
			// die, hit, idle, spellcast, strike, dead, jumpback, useitem, moveto
			if (currentMove is BattleMoveBasicAttack) {
				relativeTime = currentRealTime - currentMove.realDecisionTime

				val moveAnimation = model.skeleton.getAnimation("moveto")
				val moveTime = moveAnimation.frames.size * frameLength
				animation = moveAnimation

				if (relativeTime >= moveTime) {
					relativeTime -= moveTime

					val strikeAnimation = model.skeleton.getAnimation("strike")
					val strikeTime = strikeAnimation.frames.size * frameLength
					animation = strikeAnimation

					if (relativeTime >= strikeTime) {
						relativeTime -= strikeTime
						currentMove.finishedStrike = true

						val jumpAnimation = model.skeleton.getAnimation("jumpback")
						val jumpTime = jumpAnimation.frames.size * frameLength
						animation = jumpAnimation

						if (relativeTime >= jumpTime) {
							relativeTime = jumpTime - 1L
							currentMove.finishedJump = true
						}
					}
				} else {
					val movementProgress = relativeTime.toDouble() / moveTime.toDouble()
					//coordinates = transformBattleCoordinates(rawPosition, flipX, context.targetImage)
					val (rawTargetCoordinates, targetFlipX) = getRawCoordinates(currentMove.target)
					val targetCoordinates = transformBattleCoordinates(rawTargetCoordinates, targetFlipX, context.targetImage)
					val targetStrikePoint = getModel(currentMove.target).skeleton.strikePoint
					val strikePosition = PartyLayoutPosition(
						rawTargetCoordinates.x + (targetCoordinates.scaleX * targetStrikePoint.x).roundToInt(),
						rawTargetCoordinates.y + (targetCoordinates.scaleY * targetStrikePoint.y).roundToInt()
					)
					println("raw target position is $rawTargetCoordinates and extraX is ${targetCoordinates.scaleX * targetStrikePoint.x}")
					rawPosition = strikePosition
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

	private fun getRawCoordinates(combatant: CombatantReference): Pair<PartyLayoutPosition, Float> {
		val partyLayout = if (combatant.isPlayer) context.battle.playerLayout else context.battle.battle.enemyLayout
		val rawPosition = partyLayout.positions[combatant.index]
		val flipX = if (combatant.isPlayer) 1f else -1f
		return Pair(rawPosition, flipX)
	}

	private fun getModel(combatant: CombatantReference): BattleModel {
		return if (combatant.isPlayer) context.battle.players[combatant.index]!!.battleModel
		else context.battle.enemies[combatant.index]!!.monster.model
	}
}
