package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import mardek.content.animation.ColorTransform
import mardek.content.particle.ParticleEffect
import mardek.state.ingame.battle.EffectParticlesState
import mardek.state.ingame.battle.ParticleEmitterState
import mardek.state.util.Rectangle
import mardek.state.util.RenderTiming
import mardek.content.util.Time
import org.joml.Math.toRadians
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.min

private fun renderParticleEmitter(
	emitter: ParticleEmitterState, emitterIndex: Int,
	timing: RenderTiming, imageBatch: Vk2dImageBatch, region: Rectangle,
	pixelX: Float, pixelY: Float,
	particleEffect: ParticleEffect?, mirrorX: Boolean,
) {
	for (particle in emitter.particles) {
		val timeSinceSpawn = timing.elapsedTimeSince(particle.spawnTime)
		val alpha = min(1f, emitter.emitter.opacity.compute(timeSinceSpawn))
		if (alpha <= 0f) continue

		val relativeX = particle.computeX(timing)
		val relativeY = particle.computeY(timing)
		val particleMatrix = Matrix3x2f()
		if (mirrorX) particleMatrix.scale(-1f, 1f)
		particleMatrix.translate(emitter.emitter.transform.x, emitter.emitter.transform.y)
			.rotate(toRadians(emitter.emitter.transform.rotation))
			.translate(relativeX, relativeY)
			.rotate(toRadians(particle.computeRotation(timing)))
			.scale(region.height.toFloat())

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
				(rawCorner.first - 0.5f) * particle.computeWidth(timing),
				(rawCorner.second - 0.5f) * particle.computeHeight(timing),
			))

			Vector2f(pixelX + position.x, pixelY + position.y)
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
			it.update(updateContext, context.timing)
		}

		for (particleEffect in battle.particles) {
			for ((index, emitter) in particleEffect.emitters.withIndex()) {
				renderParticleEmitter(
					emitter, index, context.timing, imageBatch, region,
					particleEffect.position.x, particleEffect.position.y,
					particleEffect.particle, particleEffect.mirrorX,
				)
			}
		}
	}
}

internal fun renderAnimationParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch, region: Rectangle) {
	battleContext.run {
		for (combatant in battle.livingOpponents() + battle.livingPlayers()) {
			for (state in combatant.renderInfo.animationParticles.values) {
				if (state.firstRenderTime == Time.ZERO) state.firstRenderTime = context.timing.now()
				state.emitterState.update(state.firstRenderTime, context.timing)

				for (position in state.positions)	{
					renderParticleEmitter(
						state.emitterState, 0,
						context.timing, imageBatch, region,
						position.x, position.y,
						null, false,
					)
				}
			}
		}
	}
}

internal fun renderEffectParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch, region: Rectangle) {
	battleContext.run {
		for (combatant in battle.livingOpponents() + battle.livingPlayers()) {
			combatant.renderInfo.statusEffectParticles.keys.removeIf { !combatant.statusEffects.contains(it) }
			for (effect in combatant.statusEffects) {
				val effectParticlesState = combatant.renderInfo.statusEffectParticles.computeIfAbsent(
					effect
				) { EffectParticlesState(context.timing.now(), effect.particleEmitters)  }

				effectParticlesState.update(context.timing)
				for (emitterState in effectParticlesState.emitterStates) {
					renderParticleEmitter(
						emitterState, 0,
						context.timing, imageBatch, region,
						combatant.renderInfo.statusEffectPoint.x,
						combatant.renderInfo.statusEffectPoint.y,
						null, combatant.isOnPlayerSide,
					)
				}
			}
		}
	}
}
