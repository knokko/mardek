package mardek.state.ingame.battle

import mardek.content.particle.ParticleEmitter
import mardek.state.util.RenderTiming
import mardek.content.util.Time

/**
 * This class tracks the state of the particles and emitters for a status effect of a single combatant.
 */
class EffectParticlesState(

	/**
	 * The time at which the particle effects of this status effect were rendered for the first time.
	 * (Normally, this should be almost right after the status effect was given to the combatant.)
	 */
	val firstRenderTime: Time,
	emitters: Array<ParticleEmitter>,
) {

	/**
	 * The state of each of the particle emitters of the status effect.
	 */
	val emitterStates = emitters.map(::ParticleEmitterState)

	/**
	 * Updates each particle emitter
	 */
	fun update(timing: RenderTiming) {
		for (state in emitterStates) {
			state.update(firstRenderTime, timing)
		}
	}
}
