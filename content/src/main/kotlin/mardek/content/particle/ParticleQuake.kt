package mardek.content.particle

import kotlin.time.Duration

class ParticleQuake(
	val strength: Int,
	val duration: Duration,
	/**
	 * The decay per second
	 */
	val decay: Float,
) {
}
