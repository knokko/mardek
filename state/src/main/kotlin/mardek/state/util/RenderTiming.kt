package mardek.state.util

import mardek.content.util.Time
import mardek.content.util.rem
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * This class is used to compare the frame render time with the virtual in-game time.
 * It can e.g. estimate the time difference between an in-game timestamp,
 * and the moment where a frame will appear on the monitor.
 *
 * It also provides some utility methods like [oscillate] and [interpolate] that are frequently needed by the renderer.
 */
class RenderTiming(

	/**
	 * The current value of [mardek.state.ingame.CampaignState.time] or [mardek.state.ingame.area.AreaState.currentTime]
	 */
	private val stateTime: Time,

	/**
	 * The estimated result of `System.nanoTime()` when the current frame will become visible on the monitor
	 */
	val renderNanoTime: Long,

	/**
	 * The maximum amount of time that we try to extrapolate (go ahead of [stateTime])
	 */
	val extrapolationLimit: Duration,
) {

	/**
	 * Estimates the `Time` instance that most accurately represents the time the currently-being-rendered frame will
	 * appear on the monitor.
	 */
	fun now() = Time(stateTime.virtual, renderNanoTime)

	/**
	 * Estimated the amount of time between [pastReferenceTime] and
	 */
	fun elapsedTimeSince(pastReferenceTime: Time) = pastReferenceTime.elapsedSince(
		renderNanoTime, stateTime, extrapolationLimit
	)

	/**
	 * Linearly interpolates a value between two time instants. Assuming that:
	 * - the value was [startValue] at [startTime], and
	 * - the value was [finishValue] at [startTime] + [duration],
	 *
	 * this method computes the value at [now].
	 *
	 * If [clamp] is true, the value will be clamped between [startValue] and [finishValue]
	 */
	fun interpolate(startTime: Time, startValue: Float, duration: Duration, finishValue: Float, clamp: Boolean): Float {
		val elapsedTimeSinceStart = elapsedTimeSince(startTime)
		var progress = elapsedTimeSinceStart / duration
		if (!progress.isFinite()) return startValue
		if (clamp) progress = progress.coerceIn(0.0, 1.0)
		return ((1.0 - progress) * startValue + progress * finishValue).toFloat()
	}

	/**
	 * Linearly interpolates a value between two time instants. Assuming that:
	 * - the value was [startValue] at [startTime], and
	 * - the value was [finishValue] at [startTime] + [duration],
	 *
	 * this method computes the value at [now].
	 *
	 * If [clamp] is true, the value will be clamped between [startValue] and [finishValue]
	 */
	fun interpolate(startTime: Time, startValue: Int, duration: Duration, finishValue: Int, clamp: Boolean) = interpolate(
		startTime, startValue.toFloat(), duration, finishValue.toFloat(), clamp
	).roundToInt()

	/**
	 * Returns a value between [valueA] and [valueB], depending on the amount of time between [referenceTime] and [now].
	 * If this method is called every frame, the results of this method will oscillate between [valueA] and [valueB].
	 * Some examples:
	 * - it will return [valueA] when the current time is equal to the [referenceTime],
	 * or `referenceTime + anyInteger * period`
	 * - it will return `0.8 * valueA + 0.2 * valueB` when the current time is `referenceTime + 0.1 * period`,
	 * or `referenceTime + anyInteger * period + 0.1 * period`
	 * - it will return `0.6 * valueA + 0.4 * valueB` when the current time is `referenceTime + 0.2 * period`,
	 * or `referenceTime + anyInteger * period + 0.2 * period`
	 * - ...
	 * - it will return [valueB] when the current time is `referenceTime + 0.5 * period`,
	 * or `referenceTime + anyInteger * period + 0.5 * period`
	 * - it will return `0.2 * valueA + 0.8 * valueB` when the current time is `referenceTime + 0.6 * period`,
	 * or `referenceTime + anyInteger * period + 0.6 * period`
	 * - ...
	 * - it will return `0.8 * valueA + 0.2 * valueB` when the current time is `referenceTime + 0.9 * period`,
	 * or `referenceTime + anyInteger * period + 0.9 * period`
	 * - it will return `valueA` when the current time is `referenceTime + period`...
	 *
	 * Note that the [referenceTime] parameter is optional, and only needed if you care about the oscillation *phase*.
	 */
	fun oscillate(valueA: Float, valueB: Float, period: Duration, referenceTime: Time = Time.ZERO): Float {
		val passedTime = elapsedTimeSince(referenceTime).absoluteValue
		val moduloTime = passedTime % period
		val relativeTime = moduloTime / period
		val periodicRelativeTime = 2.0 * abs(0.5 - relativeTime)
		return (valueB + periodicRelativeTime * (valueA - valueB)).toFloat()
	}

	/**
	 * Calls [oscillate] with the default period of the oscillating crystal pointer.
	 */
	fun oscillateCrystalPointer(valueA: Float, valueB: Float) = oscillate(valueA, valueB, 500.milliseconds)

	/**
	 * Returns an element of [elements], depending on the amount of time between [referenceTime] and [now].
	 * If this method is called repeatedly, it will return a different element every `period / elements.length`.
	 * More precisely, if it returns the Nth element at time t,
	 * it will return the ((N + 1) % elements.length)th element at time `t + 1`.
	 *
	 * It will return `elements[0]` between `referenceTime` (inclusive) and `referenceTime + period / elements.length`
	 * (exclusive). At `referenceTime + period / elements.length`, it will return `elements[1]`, etc...
	 */
	fun <T> alternate(elements: Array<T>, period: Duration, referenceTime: Time = Time.ZERO): T {
		val passedTime = elapsedTimeSince(referenceTime)
		var moduloTime = passedTime % period
		if (moduloTime < Duration.ZERO) moduloTime += period
		val relativeTime = moduloTime / period

		// 1 element: index = 0
		// 2 elements: index = if (relativeTime < 0.5) 0 else 1
		// 3 elements: index = if (relativeTime < 0.33) 0 else if (relativeTime < 0.67) 1 else 2
		val index = (relativeTime * elements.size).toInt().coerceIn(elements.indices)
		return elements[index]
	}

	/**
	 * Calls [alternate] to get an integer between [firstElement] (inclusive) and [firstElement] + [numElements]
	 * (exclusive), with a period of [numElements] * [timePerElement].
	 */
	fun alternateIntegers(
		numElements: Int, timePerElement: Duration,
		firstElement: Int = 0, referenceTime: Time = Time.ZERO
	) = alternate(
		(firstElement until firstElement + numElements).toList().toTypedArray(),
		timePerElement * numElements, referenceTime
	)

	/**
	 * Calls [alternate] to determine the right 'walking sprite index' for area characters. This method will return
	 * either `baseIndex` or `baseIndex + 1`. If you call it repeatedly, the result will 'flip' every 350 milliseconds.
	 */
	fun walkingSpriteIndex(baseIndex: Int = 0, referenceTime: Time = Time.ZERO) = alternate(
		arrayOf(baseIndex, baseIndex + 1), 700.milliseconds, referenceTime
	)
}
