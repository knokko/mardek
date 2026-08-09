package mardek.state.ingame.battle.combatant

import mardek.content.stats.Element
import mardek.content.util.Time
import mardek.state.util.RenderTiming
import java.util.Objects
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Tracks the HP or MP history of a combatant (e.g. how much damage it took recently).
 *
 * This information is used to render e.g. damage indicators and to animate losing health/mana in the health/mana bar.
 */
class ResourceBarHistory {

	private val entries = mutableListOf<Entry>()

	/**
	 * Notifies this history that the tracked value changed from [previousValue] to [nextValue] at [currentTime].
	 */
	fun insert(previousValue: Int, nextValue: Int, currentTime: Time, element: Element) {
		if (previousValue != nextValue) {
			this.entries.add(Entry(previousValue, nextValue, currentTime, element))
		}
	}

	/**
	 * Gets the value that should be displayed at `timing.now()`, as well as an optional 'red bar' that should be
	 * rendered if the value recently dropped.
	 */
	fun get(currentValue: Int, timing: RenderTiming): Result {
		if (entries.isEmpty()) return Result(currentValue, null)

		var lastValue = entries[0].oldValue.toDouble()
		var redAlpha = 0

		for ((index, currentEntry) in entries.withIndex()) {
			val elapsedSinceStart = timing.elapsedTimeSince(currentEntry.changedAt)
			if (elapsedSinceStart <= Duration.ZERO) break

			val timeUntilFinish = reachesNewValueAfter(
				lastValue, currentEntry.newValue.toDouble()
			).seconds
			val durationToDisplay: Duration
			var timeUntilNext = timeUntilFinish

			if (index == entries.size - 1) {

				// Simple case: this is the last damage history entry
				durationToDisplay = elapsedSinceStart
			} else {
				val nextEntry = entries[index + 1]
				timeUntilNext = nextEntry.changedAt.virtualOffset(currentEntry.changedAt)

				durationToDisplay = if (timeUntilFinish <= timeUntilNext) {

					// Also simple case: there is no overlap with the next damage history entry
					elapsedSinceStart
				} else {

					// Complicated case: the next damage entry starts before this entry is finished
					timeUntilNext
				}
			}

			val nextValue = computeDisplayedValue(
				lastValue,
				currentEntry.newValue.toDouble(),
				durationToDisplay.toDouble(DurationUnit.SECONDS),
			)

			if (nextValue <= lastValue) {
				val startFadingAt = currentEntry.changedAt.virtualAdd(timeUntilNext)
				redAlpha = max(redAlpha, timing.interpolate(
					startFadingAt, 255, RED_FADE_DURATION, 0, true
				))
			}

			lastValue = nextValue
		}

		val displayedValue = lastValue.roundToInt()
		val redBar = if (redAlpha > 0 && entries[0].oldValue > displayedValue) {
			RedBar(displayedValue, entries[0].oldValue, redAlpha)
		} else null

		if (redAlpha == 0 && displayedValue == currentValue) entries.clear()
		return Result(displayedValue, redBar)
	}

	private class Entry(
		val oldValue: Int,
		val newValue: Int,
		val changedAt: Time,
		val element: Element,
	)

	/**
	 * Represents a 'red bar' that should be rendered on the health bar when a combatant recently lost health.
	 * This is used in [Result].
	 */
	class RedBar(
		/**
		 * The new health
		 */
		val minValue: Int,

		/**
		 * The old health
		 */
		val maxValue: Int,

		/**
		 * The alpha (opacity) that the red bar should have (the red bar starts fading after a while)
		 */
		val alpha: Int,
	) {
		override fun equals(other: Any?) = other is RedBar && minValue == other.minValue &&
				maxValue == other.maxValue && alpha == other.alpha

		override fun hashCode() = minValue - 13 * maxValue + 127 * alpha
	}

	/**
	 * The return type of [get]
	 */
	class Result(

		/**
		 * The health or many that should be displayed
		 */
		val displayedValue: Int,

		/**
		 * An optional [RedBar]: only when the health recently dropped
		 */
		val bar: RedBar?,
	) {
		override fun equals(other: Any?) = other is Result && displayedValue == other.displayedValue && bar == other.bar

		override fun hashCode() = displayedValue - 32 * Objects.hashCode(bar)
	}

	companion object {

		private val RED_FADE_DURATION = 1500.milliseconds

		// I'm looking for a smooth formula that approximates the vanilla formula, which is:
		// - 1 to 8 damage: 1 hp per frame -> 30 hp per second
		// - 9 to 20 damage: 3 hp per frame -> 90 hp per second
		// - 21 to 200 damage: 8 hp per frame -> 240 hp per second
		// - 201+ damage: 80 hp per frame -> 2400 hp per second

		// Lets generalize it to f'(x) = a * (b + newHealth - f(x))^c

		// Some tuning attempts:
		// (12, 2, 0.7)  -> 0.11, 0.23, 0.32, 1.5, 3.6
		// (12, 2, 0.75) -> 0.10, 0.21, 0.35, 1.2, 2.7
		// (10, 2, 0.8)  -> 0.12, 0.23, 0.38, 1.2, 2.4
		// (10, 1, 0.8)  -> 0.16, 0.29, 0.43, 1.3, 2.5
		// desired =        0.10, 0.30, 0.35, 1.2, 3.6

		private const val A = 12.0
		private const val B = 2.0
		private const val C = 0.75

		// According to ChatGPT, this is the corresponding formula for f(x):
		// f(x) = b + newHealth - [(b + newHealth - oldHealth)^(1 - c) - (1 - c)ax]^(1 / (1 - c))

		// Lets simplify it to f(x) = b + newHealth - [(b + newHealth - oldHealth)^d - dax]^(1 / d)
		private const val D = 1.0 - C

		internal fun computeDisplayedValue(oldHealth: Double, newHealth: Double, elapsedSeconds: Double): Double {
			return if (newHealth >= oldHealth) {
				val innerClause = (B + newHealth - oldHealth).pow(D) - D * A * elapsedSeconds
				if (innerClause <= 0.0) newHealth
				else min(newHealth, B + newHealth - innerClause.pow(1.0 / D))
			} else {
				val innerClause = (B + oldHealth - newHealth).pow(D) - D * A * elapsedSeconds
				if (innerClause <= 0.0) newHealth
				else max(newHealth, -B + newHealth + innerClause.pow(1.0 / D))
			}
		}

		// Solve f(x) = b + newHealth - [(b + newHealth - oldHealth)^d - dax]^(1 / d) = newHealth ->
		//       [(b + newHealth - oldHealth)^d - dax]^(1 / d) = b ->
		//       (b + newHealth - oldHealth)^d - dax = b^d ->
		//       -dax = b^d - (b + newHealth - oldHealth)^d ->
		//       dax = (b + newHealth - oldHealth)^d - b^d ->
		//       x = [(b + newHealth - oldHealth)^d - b^d] / ad
		internal fun reachesNewValueAfter(oldHealth: Double, newHealth: Double): Double {
			return if (newHealth >= oldHealth) {
				((B + newHealth - oldHealth).pow(D) - B.pow(D)) / (A * D)
			} else {
				((B + oldHealth - newHealth).pow(D) - B.pow(D)) / (A * D)
			}
		}
	}
}
