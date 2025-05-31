package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.animations.ColorTransform
import org.joml.Math.toRadians
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.min

class ParticleRenderer(private val context: BattleRenderContext) {

	private val renderTime = System.nanoTime()

	fun render() {
		if (context.battle.particles.isEmpty()) return
		context.battle.particles.removeIf {
			it.update(context.updateContext, renderTime)
		}
		context.resources.partRenderer.startBatch(context.recorder)
		for (particleEffect in context.battle.particles) {
			val flipX = if (particleEffect.combatant.isOnPlayerSide) 1f else -1f
			val combatantPosition = transformBattleCoordinates(
				particleEffect.combatant.getPosition(context.battle),
				flipX, context.targetImage
			)
			for (emitter in particleEffect.emitters) {
				for (particle in emitter.particles) {
					val relativeX = particle.computeX(renderTime)
					val relativeY = particle.computeY(renderTime)
					val particleMatrix = Matrix3x2f().translate(
						emitter.emitter.transform.x, emitter.emitter.transform.y
					).rotate(toRadians(emitter.emitter.transform.rotation))
						.translate(relativeX, relativeY)
						.rotate(particle.computeRotation(renderTime))

					val timeSinceSpawn = (renderTime - particle.spawnTime) / 1000_000_000f
					val alpha = min(1f, emitter.emitter.opacity.compute(timeSinceSpawn))
					if (alpha <= 0f) continue
					val fadeTransform = ColorTransform(0, rgba(1f, 1f, 1f, alpha))

					val corners = arrayOf(
						Pair(0f, 0f),
						Pair(1f, 0f),
						Pair(1f, 1f),
						Pair(0f, 1f)
					).map { rawCorner ->
						val position = particleMatrix.transformPosition(Vector2f(
							(rawCorner.first - 0.5f) * emitter.emitter.size.baseWidth,
							(rawCorner.second - 0.5f) * emitter.emitter.size.baseHeight,
						))

						Vector2f(
							combatantPosition.x + position.x * combatantPosition.scaleX,
							combatantPosition.y + position.y * combatantPosition.scaleY
						)
					}.toTypedArray()
					context.resources.partRenderer.render(particle.emitter.sprite.sprite, corners, fadeTransform)
				}
			}
		}
		context.resources.partRenderer.endBatch()
	}
}
