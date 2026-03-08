package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import mardek.content.animation.ColorTransform
import mardek.content.particle.ParticleEffect
import mardek.state.ingame.battle.EffectParticlesState
import mardek.state.ingame.battle.ParticleEmitterState
import mardek.state.util.Rectangle
import org.joml.Math.toRadians
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.min

private fun renderParticleEmitter(
	emitter: ParticleEmitterState, emitterIndex: Int,
	renderTime: Long, imageBatch: Vk2dImageBatch, magicScale: Float,
	pixelX: Float, pixelY: Float,
	particleEffect: ParticleEffect?, mirrorX: Boolean,
) {
	for (particle in emitter.particles) {
		val timeSinceSpawn = (renderTime - particle.spawnTime) / 1000_000_000f
		val alpha = min(1f, emitter.emitter.opacity.compute(timeSinceSpawn))
		if (alpha <= 0f) continue

		val relativeX = particle.computeX(renderTime)
		val relativeY = particle.computeY(renderTime)
		val particleMatrix = Matrix3x2f()
		if (mirrorX) particleMatrix.scale(-1f, 1f)
		particleMatrix.translate(emitter.emitter.transform.x, emitter.emitter.transform.y)
			.rotate(toRadians(emitter.emitter.transform.rotation))
			.translate(relativeX, relativeY)
			.rotate(toRadians(particle.computeRotation(renderTime)))

		val fadeTransform = ColorTransform(
			0, rgba(1f, 1f, 1f, alpha), 0
		)

		val corners = arrayOf(
			Pair(0f, 0f),
			Pair(1f, 0f),
			Pair(1f, 1f),
			Pair(0f, 1f)
		).map { rawCorner ->
			val position = particleMatrix.transformPosition(Vector2f(
				(rawCorner.first - 0.5f) * particle.computeWidth(renderTime),
				(rawCorner.second - 0.5f) * particle.computeHeight(renderTime),
			))

			Vector2f(pixelX + position.x * magicScale, pixelY+ position.y * magicScale)
		}.toTypedArray()
		val overrideSprites = particleEffect?.inheritance?.overrideSprites
		val sprite = if (overrideSprites != null) {
			overrideSprites[emitterIndex].sprite
		} else emitter.emitter.sprite.sprite

		imageBatch.transformed(
			corners[0].x, corners[0].y,
			corners[1].x, corners[1].y,
			corners[2].x, corners[2].y,
			corners[3].x, corners[3].y,
			sprite.index,
			fadeTransform.addColor,
			fadeTransform.multiplyColor,
		)
	}
}

internal fun renderBaseParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch, region: Rectangle) {
	battleContext.run {
		battle.particles.removeIf {
			it.update(updateContext, renderTime)
		}

		for (particleEffect in battle.particles) {
			val magicScale = computeMagicScale(region)

			for ((index, emitter) in particleEffect.emitters.withIndex()) {
				renderParticleEmitter(
					emitter, index, renderTime, imageBatch, magicScale,
					particleEffect.position.x, particleEffect.position.y,
					particleEffect.particle, particleEffect.mirrorX,
				)
			}
		}
	}
}

internal fun renderEffectParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch, region: Rectangle) {
	battleContext.run {
		val magicScale = computeMagicScale(region)
		val renderTime = System.nanoTime()

		for (combatant in battle.livingOpponents() + battle.livingPlayers()) {
			combatant.renderInfo.statusEffectParticles.keys.removeIf { !combatant.statusEffects.contains(it) }
			for (effect in combatant.statusEffects) {
				val effectParticlesState = combatant.renderInfo.statusEffectParticles.computeIfAbsent(
					effect
				) { EffectParticlesState(renderTime, effect.particleEmitters)  }

				effectParticlesState.update(renderTime)
				for (emitterState in effectParticlesState.emitterStates) {
					renderParticleEmitter(
						emitterState, 0,
						renderTime, imageBatch, magicScale,
						combatant.renderInfo.statusEffectPoint.x,
						combatant.renderInfo.statusEffectPoint.y,
						null, false,
					)
				}
			}
		}
	}
}
