package mardek.content.util

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import kotlin.math.min
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Represents a virtual/in-game time instant, accompanied by the real time (`System.nanoTime()`) at which the `Time`
 * instance was constructed.
 *
 * ### Purpose
 * This class is used to represent the current `CampaignState.time` and `AreaState.currentTime`.
 * It is also used to remember time instances in the past, and compare it with the current time,
 * to check how much time has elapsed in the meantime.
 *
 * ### Virtual time
 * The [virtual] time is the *leading* time component that is used to update the game state.
 * It is increased by the *update time step* during each `InGameState.update`,
 * and completely independent of the real time measured by `System.nanoTime()` and `System.currentTimeMillis()`.
 * This is useful because it allows unit tests to 'speed up' the time by calling `InGameState.update()` more often.
 *
 * ### Real time
 * The real [nanoTime] is sometimes used by the renderer for extrapolation.
 * The `RenderTiming` class uses this to create slightly smoother fade-ins, fade-outs, and animations.
 * This field would be extra useful if the state update frequency is reduced in the future,
 * or when Vulkan extensions are used to accurately predict the time at which the next
 * frame becomes visible on the monitor.
 */
@BitStruct(backwardCompatible = true)
class Time(

	/**
	 * The virtual/in-game time that has elapsed since the 'origin'
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val virtual: Duration,

	/**
	 * The (initial) value of [nanoTime]
	 */
	initialNanoTime: Long = System.nanoTime()
): BitPostInit {

	/**
	 * The real time (result of `System.nanoTime()`) at which this `Time` instance was constructed.
	 */
	var nanoTime = initialNanoTime
		private set

	@Suppress("unused")
	private constructor() : this(Duration.ZERO)

	override fun toString() = "Time(virtual=$virtual, nano=$nanoTime)"

	operator fun plus(right: Duration) = Time(virtual + right)

	override fun equals(other: Any?) = other is Time && this.virtual == other.virtual && this.nanoTime == other.nanoTime

	override fun hashCode() = virtual.hashCode() - 31 * nanoTime.hashCode()

	/**
	 * Returns the virtual/in-game time that has elapsed between `this` and `referenceTime`:
	 * it is simply `this.virtual - referenceTime.virtual`.
	 */
	fun virtualOffset(referenceTime: Time) = this.virtual - referenceTime.virtual

	/**
	 * Returns a `Time` instance whose *virtual* time is `this.virtual + right`, and whose `nanoTime` is `this.nanoTime`
	 */
	fun virtualAdd(right: Duration) = Time(this.virtual + right, this.nanoTime)

	/**
	 * Assuming that `currentNanoTime == System.nanoTime()`,
	 * computes the amount of time that has elapsed between the current time and `this` time,
	 * taking both the virtual time and real time into account.
	 *
	 * The `mostRecentUpdate` time should be the current value of `CampaignState.time` or `AreaState.currentTime`.
	 * The difference between `mostRecentUpdate.virtual` and `this.virtual` is used to determine how much (in-game)
	 * time has certainly elapsed.
	 *
	 * The difference between `mostRecentUpdate.nanoTime` and `currentNanoTime`,
	 * or the difference between `this.nanoTime` and `currentNanoTime`,
	 * is used to determine how much *real* time has elapsed between
	 * the current time and the most-recent `Time` instance.
	 * This is used for extrapolation, and will be limited to [extrapolationLimit] when it's too large.
	 */
	fun elapsedSince(currentNanoTime: Long, mostRecentUpdate: Time, extrapolationLimit: Duration): Duration {
		val virtualOffset = mostRecentUpdate.virtualOffset(this)
		val realOffset = (currentNanoTime - mostRecentUpdate.nanoTime).nanoseconds.coerceIn(
			-extrapolationLimit .. extrapolationLimit
		)

		return if (virtualOffset > Duration.ZERO) {

			// Expected case: mostRecentUpdate is later than `this` time
			virtualOffset + realOffset
		} else if (virtualOffset < Duration.ZERO) {

			// Special/rare case: mostRecentUpdate is earlier than `this` time

			// This is only possible when `this` time was artificially created, e.g. by `someOldTime + someDuration`
			// when the difference between `mostRecentUpdate` and `someOldTime` is less than `someDuration`.
			val timeInFuture = virtualOffset.absoluteValue - realOffset
			-timeInFuture
		} else {

			// Special/rare case: mostRecentUpdate has the same *virtual* time as `this` time.
			// This usually indicates that `this` time was created using `timing.now()`.
			(currentNanoTime - max(this.nanoTime, mostRecentUpdate.nanoTime)).nanoseconds.coerceIn(
				-extrapolationLimit .. extrapolationLimit
			)
		}
	}

	override fun postInit(context: BitPostInit.Context) {
		this.nanoTime = 0L
	}

	companion object {

		/**
		 * The 'dummy' zero time: this is a `Time` instance where both its virtual time and real time are zero.
		 * This instance is occasionally used to represent an unknown or uninitialized time.
		 * It can also be used as a stable 'reference time' to render periodic animations.
		 */
		val ZERO = Time(Duration.ZERO, 0L)

		/**
		 * Creates a `Time` instance whose virtual time is zero, but whose real time ([nanoTime]) is non-zero.
		 * The [nanoTime] will be the current result of `System.nanoTime()`.
		 */
		fun zero() = Time(Duration.ZERO)
	}
}

/**
 * Returns the minimum of the two durations
 */
fun min(a: Duration, b: Duration): Duration = min(a.inWholeNanoseconds, b.inWholeNanoseconds).nanoseconds

/**
 * Returns the maximum of the two durations
 */
fun max(a: Duration, b: Duration): Duration = max(a.inWholeNanoseconds, b.inWholeNanoseconds).nanoseconds

operator fun Duration.rem(right: Duration) = (this.inWholeNanoseconds % right.inWholeNanoseconds).nanoseconds
