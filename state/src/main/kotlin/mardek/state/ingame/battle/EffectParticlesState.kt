package mardek.state.ingame.battle

import mardek.content.particle.ParticleEmitter

/**
 * This class tracks the state of the particles and emitters for a status effect of a single combatant.
 */
class EffectParticlesState(

	/**
	 * The time (`System.nanoTime()`) at which the particle effects of this status effect were rendered for the
	 * first time. (Normally, this should be almost right after the status effect was given to the combatant.)
	 */
	val firstRenderTime: Long,
	emitters: Array<ParticleEmitter>,
) {

	/**
	 * The state of each of the particle emitters of the status effect.
	 */
	val emitterStates = emitters.map(::ParticleEmitterState)

	/**
	 * Updates each particle emitter
	 */
	fun update(renderTime: Long) {
		for (state in emitterStates) {
			state.update(firstRenderTime, renderTime)
		}
	}
}
