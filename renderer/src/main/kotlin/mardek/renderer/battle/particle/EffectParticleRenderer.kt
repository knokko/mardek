package mardek.renderer.battle.particle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.animations.ColorTransform
import mardek.content.stats.StatusEffect
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.battle.computeMagicScale
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.CustomParticle
import org.joml.Math.toRadians
import org.joml.Vector2f
import kotlin.math.cos
import kotlin.math.sin

class EffectParticleRenderer(private val context: BattleRenderContext) {

	fun render() {
		for (combatant in context.battle.livingOpponents() + context.battle.livingPlayers()) {
			for (effect in combatant.statusEffects) emit(combatant, effect)
		}

		val magicScale = computeMagicScale(context.targetImage)
		val renderTime = System.nanoTime()
		context.battle.customParticles.removeIf { particle ->
			val rotation = toRadians(particle.rotation)
			val opacity = particle.getOpacity(renderTime)
			val size = particle.getSize(renderTime)
			val h = 0.5f
			val corners = arrayOf(
				Vector2f(-h, -h), Vector2f(h, -h),
				Vector2f(h, h), Vector2f(-h, h)
			).map { rawCorner ->
				val offsetX = particle.getX(renderTime) + size * (rawCorner.x * cos(rotation) -
						rawCorner.y * sin(rotation)) + particle.combatant.getModel().skeleton.statusPoint.x
				val offsetY = particle.getY(renderTime) + size * (rawCorner.x * sin(rotation) +
						rawCorner.y * cos(rotation)) + particle.combatant.getModel().skeleton.statusPoint.y
				Vector2f(
					particle.combatant.lastRenderedPosition.first + magicScale.first * offsetX,
					particle.combatant.lastRenderedPosition.second + magicScale.second * offsetY
				)
			}.toTypedArray()

			if (opacity > 0f && size > 0.1f) {
				context.resources.partRenderer.render(particle.sprite, corners, ColorTransform(
					0, rgba(1f, 1f, 1f, opacity)
				))
				false
			} else true
		}
	}

	private fun emit(combatant: CombatantState, effect: StatusEffect) {
		val currentTime = System.nanoTime()

		fun maybeEmit(period: Long, generate: () -> Unit) {
			val lastEmission = combatant.lastStatusEffectParticleEmissions[effect]
			if (lastEmission != null && lastEmission + period > currentTime) return

			combatant.lastStatusEffectParticleEmissions[effect] = currentTime
			generate()
		}

		if (effect.flashName == "PSN") maybeEmit(200_000_000L) {
			context.battle.customParticles.add(CustomParticle.poison(combatant, effect.passiveParticleSprites[0]))
		}
		if (effect.flashName == "DRK") maybeEmit(130_000_000L) {
			context.battle.customParticles.add(CustomParticle.blind(combatant, effect.passiveParticleSprites[0]))
		}
		if (effect.flashName == "SLP") maybeEmit(1000_000_000L) {
			context.battle.customParticles.add(CustomParticle.sleep(combatant, effect.passiveParticleSprites[0]))
		}
		if (effect.flashName == "PAR") maybeEmit(900_000_000L) {
			context.battle.customParticles.add(CustomParticle.paralysis(combatant, effect.passiveParticleSprites[0]))
		}
	}
}
