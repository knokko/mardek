package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.content.animations.BattleModel
import mardek.content.battle.PartyLayout
import mardek.content.battle.PartyLayoutPosition
import mardek.renderer.SharedResources
import mardek.state.ingame.battle.BattleState
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.lang.Math.toIntExact
import kotlin.math.min

class BattleRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val state: BattleState,
	private val resources: SharedResources,
	private val playerLayout: PartyLayout
) {

	fun render(frameIndex: Int) {
		val relativeTime = System.nanoTime() - state.startTime
		resources.partRenderer.startBatch(recorder)
		resources.partRenderer.render(state.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for ((index, enemy) in state.battle.enemies.withIndex()) {
			if (enemy == null) continue

			val rawPosition = state.battle.enemyPositions.positions[index]
			renderCreature(rawPosition, enemy.monster.model, -1f, relativeTime)
		}

		for ((index, player) in state.players.withIndex()) {
			if (player == null) continue

			renderCreature(playerLayout.positions[index], player.battleModel, 1f, relativeTime)
		}

		resources.partRenderer.endBatch()
	}

	private fun renderCreature(rawPosition: PartyLayoutPosition, model: BattleModel, flipX: Float, relativeTime: Long) {

		// Original resolution is 240x176
		var magicScaleX = 1f / 240f
		val magicScaleY2 = 1f / 176f
		val magicScaleY1 = min(magicScaleY2, magicScaleX * targetImage.width / targetImage.height)
		magicScaleX = min(magicScaleX, magicScaleY1 * targetImage.height / targetImage.width)

		val rawX = -flipX * (-1f + (rawPosition.x + 38) * magicScaleX)
		val rawRelativeY = rawPosition.y + 78 - 176
		val rawY = rawRelativeY * magicScaleY2
		val animation = model.skeleton.animations["idle"]!!

		val frameLength = 33_000_000L
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

					Vector2f(rawX + position.x * magicScaleX, rawY + position.y * magicScaleY1)
				}.toTypedArray()
				resources.partRenderer.render(entry.sprite, corners, animationPart.color)
			}
		}
	}
}
