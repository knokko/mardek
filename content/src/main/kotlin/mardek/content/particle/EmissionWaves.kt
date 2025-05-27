package mardek.content.particle

import mardek.content.audio.SoundEffect
import kotlin.time.Duration

class EmissionWaves(
	/**
	 * The delay before this emitter starts emitting particles
	 */
	val delay: Duration,

	/**
	 * The sound effect to be played after the `delay` has passed (so when particles start being emitted)
	 */
	val delayedSound: SoundEffect?,

	/**
	 * The time between spawning 2 (rounds of) particles
	 */
	val period: Duration,

	val particlesPerRound: Int,

	/**
	 * The number of rounds/periods
	 */
	val numRounds: Int,
) {
}
