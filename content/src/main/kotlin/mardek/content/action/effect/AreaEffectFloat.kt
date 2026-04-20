package mardek.content.action.effect

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * A float function that can be used by an area effect: it is a pure function whose value can only depend on a
 * parameter `t` that represents the number of seconds that has elapsed since some reference point in time.
 */
@BitStruct(backwardCompatible = true)
class AreaEffectFloat(

	/**
	 * The return value at `t = 0`
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.01)
	val initial: Float = 0f,

	/**
	 * This return value grows by `linear` every second (e.g. if the function returns `X` at `t = 0`, it will return
	 * `X + 5 * linear` at time `t = 5`).
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.01, commonValues = [0.0])
	val linear: Float = 0f,

	/**
	 * When the return value would be smaller than `min`, the return value becomes `min`. This can be used to 'clamp'
	 * the return value.
	 */
	@BitField(id = 2)
	@FloatField(commonValues = [Float.MIN_VALUE.toDouble()])
	val min: Float = Float.MIN_VALUE,

	/**
	 * When the return value would be larger than `max`, the return value becomes `max`. This can be used to 'clamp'
	 * the return value.
	 */
	@BitField(id = 3)
	@FloatField(commonValues = [Float.MAX_VALUE.toDouble()])
	val max: Float = Float.MAX_VALUE,
) {

	/**
	 * Gets the return value of this function at time `t = timeSinceStart`. The return value will be clamped to the
	 * range `[minValue, maxValue]` if needed.
	 */
	fun get(timeSinceStart: Duration, minValue: Float, maxValue: Float) = (
			initial + linear * timeSinceStart.toDouble(DurationUnit.SECONDS).toFloat()
	).coerceIn(max(minValue, this.min), min(maxValue, this.max))

	/**
	 * Gets the function value at `t = timeSinceStart`, and clamps is to the range `[0, 1]`:
	 * it returns `get(timeSinceStart, minValue = 0f, maxValue = 1f)`
	 */
	fun getColorComponent(timeSinceStart: Duration) = get(timeSinceStart, 0f, 1f)

	/**
	 * Gets the function value at `t = timeSinceStart`, or 0 when the value is negative:
	 * it returns `get(timeSinceStart, minValue = 0f, maxValue = Float.MAX_VALUE)`
	 */
	fun getNonNegative(timeSinceStart: Duration) = get(timeSinceStart, 0f, Float.MAX_VALUE)

	/**
	 * Gets the function value at `t = timeSinceStart`, without any **additional** clamping:
	 * it returns `get(timeSinceStart, minValue = -Float.MAX_VALUE, maxValue = Float.MAX_VALUE)`
	 *
	 * Note that [min] and [max] are still respected!
	 */
	fun getAny(timeSinceStart: Duration) = get(timeSinceStart, -Float.MAX_VALUE, Float.MAX_VALUE)
}
