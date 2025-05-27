package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit

@BitStruct(backwardCompatible = true)
class ParticleOpacity(
	/**
	 * The (initial) opacity of each emitted particle:
	 * - 0.0 (or lower) means that the particle is invisible
	 * - 1.0 means that the particle is opaque
	 * - values between 0.0 and 1.0 means that the particle is translucent
	 * - values above 1.0 mean that the particle becomes extra bright
	 */
	val alpha: Float,

	/**
	 * The opacity of each particle will be increased by `fade` every second (continuously)
	 */
	val fade: Float,

	/**
	 * - when `fade > 0`, this is the maximum value of the opacity in any frame
	 * - when `fade < 0`, this is the minimum value of the opacity in any frame
	 */
	val alphaLimit: Float,
) {
	fun compute(timeSinceSpawn: Duration): Float {
		val uncappedResult = alpha + fade * timeSinceSpawn.toDouble(DurationUnit.SECONDS).toFloat()
		if (fade > 0f) return min(alphaLimit, uncappedResult)
		if (fade < 0f) return max(alphaLimit, uncappedResult)
		return uncappedResult
	}
}
