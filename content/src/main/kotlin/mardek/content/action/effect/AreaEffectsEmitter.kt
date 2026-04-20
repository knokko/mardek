package mardek.content.action.effect

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import kotlin.time.Duration

/**
 * An emitter that (periodically) emits sub-effects for [AreaActionEffect]s.
 *
 * It will emit all its sub-effects at times `spawnTime + [firstDispatchAfter] + n * [period]` for all
 * `n` in `0 until [maxDispatches]]`.
 */
@BitStruct(backwardCompatible = true)
class AreaEffectsEmitter(

	/**
	 * The first sub-effect(s) will be emitted at `spawnTime + firstDispatchAfter`
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val firstDispatchAfter: Duration,

	/**
	 * When `maxDispatches > 1`, this is the time between consecutive dispatches. Otherwise, it is ignored.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val period: Duration,

	/**
	 * The maximum number of times that this emitter can emit its sub-effects,
	 * or 0 if there is no limit.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val maxDispatches: Int,

	/**
	 * An upper bound on the maximum visible 'lifetime' of each subparticle: when sub-particles are spawned at some
	 * time `t`, all of them must be invisible after `t + maxLifetime`.
	 *
	 * This variable is only used for optimizing the renderer.
	 *
	 * Choosing a `maxLifetime` that is too high may have a small negative performance impact, but shouldn't have any
	 * other consequences.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val maxLifetime: Duration,

	/**
	 * The 'ring' sub-effects that are emitted at each *dispatch*. These can be used to generate special effects in the
	 * shape of a ring or circle.
	 *
	 * Currently, these are the only types of special effects supported.
	 */
	@BitField(id = 4)
	val rings: Array<AreaRingEffect>,
) {

	init {
		if (maxDispatches > 1 && period == Duration.ZERO) {
			throw IllegalArgumentException("period must be non-zero when maxDispatches ($maxDispatches) > 1")
		}
	}

	@Suppress("unused")
	private constructor() : this(
		Duration.ZERO, Duration.ZERO,
		0, Duration.ZERO, emptyArray(),
	)

	/**
	 * Gets the dispatch indices that are potentially relevant at `spawnTime + timeSinceSpawn`.
	 * All the sub-effects of any dispatch *outside* this range are *certainly* invisible.
	 *
	 * To give an intuition, consider an emitter with:
	 * - `firstDispatchAfter = 1.seconds`
	 * - `period = 2.seconds`
	 * - `maxDispatches = 4`
	 * - `maxLifetime = 5.seconds`
	 *
	 * Before `spawnTime + 1.seconds`, the emitter will not emit any sub-effects,
	 * so `getRelevantDispatches(900.milliseconds)` returns an empty range.
	 * Calling `getRelevantDispatches(1.seconds)` will return the range `[0, 0]`,
	 * since only the sub-effects of the first dispatch (dispatch 0) are spawned after 1 second.
	 * The result of `getRelevantDispatches(2999.milliseconds)` would also be `[0, 0]`,
	 * but the result of `getRelevantDispatches(3.seconds)` would be `[0, 1]`,
	 * since the second dispatch is at `spawnTime + firstDispatchAfter + period = spawnTime + 3.seconds`.
	 *
	 * The sub-effects of the first dispatch (dispatch 0) are spawned at `spawnTime + 1.seconds`,
	 * and are certainly invisible at `spawnTime + firstDispatchAfter + maxLifetime = spawnTime + 6.seconds`.
	 * So the result of `getRelevantDispatches(6.seconds)` will no longer include 0.
	 * Instead, it would be `[1, 2]`.
	 */
	fun getRelevantDispatches(timeSinceSpawn: Duration): IntRange {
		if (timeSinceSpawn < firstDispatchAfter) return IntRange.EMPTY

		val timeSinceStart = timeSinceSpawn - firstDispatchAfter
		val firstExpiration = firstDispatchAfter + maxLifetime
		if (period == Duration.ZERO) {
			return if (maxLifetime == Duration.ZERO || timeSinceSpawn < firstExpiration) 0 .. 0
			else IntRange.EMPTY
		}

		var latestDispatch = (timeSinceStart / period).toInt()
		if (maxDispatches != 0 && latestDispatch >= maxDispatches) latestDispatch = maxDispatches - 1

		val firstRelevantDispatch = if (maxLifetime == Duration.ZERO || timeSinceSpawn < firstExpiration) 0 else {
			val timeSinceFirstExpiration = timeSinceSpawn - firstExpiration
			val lastExpiredDispatch = (timeSinceFirstExpiration / period).toInt()
			lastExpiredDispatch + 1
		}

		return firstRelevantDispatch .. latestDispatch
	}
}
