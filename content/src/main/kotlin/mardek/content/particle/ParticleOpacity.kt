package mardek.content.particle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
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
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.01)
	val initial: Float,

	/**
	 * The opacity of each particle will be increased by `grow` every second (continuously)
	 */
	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.1)
	val grow: Float,

	/**
	 * - when `grow > 0`, this is the maximum value of the opacity in any frame
	 * - when `grow < 0`, this is the minimum value of the opacity in any frame
	 */
	@BitField(id = 3, optional = true)
	@FloatField(expectMultipleOf = 0.1)
	val limit: Float?,
) {
	internal constructor() : this(0f, 0f, null)

	fun compute(timeSinceSpawn: Float): Float {
		val uncappedResult = initial + grow * timeSinceSpawn
		if (limit == null) return uncappedResult
		if (grow > 0f) return min(limit, uncappedResult)
		if (grow < 0f) return max(limit, uncappedResult)
		return uncappedResult
	}
}
