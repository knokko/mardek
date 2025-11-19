package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import mardek.content.animation.ColorTransform
import mardek.content.stats.StatusEffect
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.CustomParticle
import org.joml.Math.toRadians
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal fun renderBaseParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch) {
	battleContext.run {
		if (battle.particles.isEmpty()) return

		battle.particles.removeIf {
			it.update(updateContext, renderTime)
		}

		for (particleEffect in battle.particles) {
			val magicScale = computeMagicScale(battleContext)

			for ((index, emitter) in particleEffect.emitters.withIndex()) {
				for (particle in emitter.particles) {
					val timeSinceSpawn = (renderTime - particle.spawnTime) / 1000_000_000f
					val alpha = min(1f, emitter.emitter.opacity.compute(timeSinceSpawn))
					if (alpha <= 0f) continue

					val relativeX = particle.computeX(renderTime)
					val relativeY = particle.computeY(renderTime)
					val particleMatrix = Matrix3x2f()
					if (particleEffect.mirrorX) particleMatrix.scale(-1f, 1f)
					particleMatrix.translate(
						emitter.emitter.transform.x, emitter.emitter.transform.y
					).rotate(toRadians(emitter.emitter.transform.rotation))
						.translate(relativeX, relativeY)
						.rotate(particle.computeRotation(renderTime))

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
							(rawCorner.first - 0.5f) * particle.initialWidth * emitter.emitter.size.growX.pow(timeSinceSpawn),
							(rawCorner.second - 0.5f) * particle.initialHeight * emitter.emitter.size.growY.pow(timeSinceSpawn),
						))

						Vector2f(
							particleEffect.position.x + position.x * magicScale,
							particleEffect.position.y + position.y * magicScale,
						)
					}.toTypedArray()
					val overrideSprites = particleEffect.particle.inheritance?.overrideSprites
					val sprite = if (overrideSprites != null) {
						overrideSprites[index].sprite
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
		}
	}
}

internal fun renderEffectParticles(battleContext: BattleRenderContext, imageBatch: Vk2dImageBatch) {
	battleContext.run {
		for (combatant in battle.livingOpponents() + battle.livingPlayers()) {
			for (effect in combatant.statusEffects) emit(battleContext, combatant, effect)
		}

		val magicScale = computeMagicScale(battleContext)
		val renderTime = System.nanoTime()
		battle.customParticles.removeIf { particle ->
			val rotation = toRadians(particle.rotation)
			val opacity = particle.getOpacity(renderTime)
			val size = particle.getSize(renderTime)
			val h = 0.5f
			val corners = arrayOf(
				Vector2f(-h, h), Vector2f(h, h),
				Vector2f(h, -h), Vector2f(-h, -h),
			).map { rawCorner ->
				val offsetX = particle.getX(renderTime) + size * (rawCorner.x * cos(rotation) -
						rawCorner.y * sin(rotation))
				val offsetY = particle.getY(renderTime) + size * (rawCorner.x * sin(rotation) +
						rawCorner.y * cos(rotation) - 0.5f)
				Vector2f(
					particle.combatant.renderInfo.statusEffectPoint.x + magicScale * offsetX,
					particle.combatant.renderInfo.statusEffectPoint.y + magicScale * offsetY
				)
			}.toTypedArray()

			if (opacity > 0f && size > 0.1f) {
				imageBatch.transformed(
					corners[0].x, corners[0].y,
					corners[1].x, corners[1].y,
					corners[2].x, corners[2].y,
					corners[3].x, corners[3].y,
					particle.sprite.index,
					0, rgba(1f, 1f, 1f, opacity * 0.6f),
				)
				false
			} else true
		}
	}
}

private fun emit(battleContext: BattleRenderContext, combatant: CombatantState, effect: StatusEffect) {
	val currentTime = System.nanoTime()

	fun maybeEmit(period: Long, generate: () -> Unit) {
		val lastEmission = combatant.renderInfo.lastStatusEffectParticleEmissions[effect]
		if (lastEmission != null && lastEmission + period > currentTime) return

		combatant.renderInfo.lastStatusEffectParticleEmissions[effect] = currentTime
		generate()
	}

	if (effect.flashName == "PSN") maybeEmit(200_000_000L) {
		battleContext.battle.customParticles.add(CustomParticle.poison(combatant, effect.passiveParticleSprites[0]))
	}
	if (effect.flashName == "DRK") maybeEmit(130_000_000L) {
		battleContext.battle.customParticles.add(CustomParticle.blind(combatant, effect.passiveParticleSprites[0]))
	}
	if (effect.flashName == "SLP") maybeEmit(1000_000_000L) {
		battleContext.battle.customParticles.add(CustomParticle.sleep(combatant, effect.passiveParticleSprites[0]))
	}
	if (effect.flashName == "PAR") maybeEmit(900_000_000L) {
		battleContext.battle.customParticles.add(CustomParticle.paralysis(combatant, effect.passiveParticleSprites[0]))
	}
}
