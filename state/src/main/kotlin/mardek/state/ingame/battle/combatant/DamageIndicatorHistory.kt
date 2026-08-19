package mardek.state.ingame.battle.combatant

import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import mardek.content.util.Time
import mardek.content.util.max
import mardek.state.ingame.battle.MoveResult
import mardek.state.util.RenderTiming
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tracks the health & mana (or level-ups) that this combatant recently gained or lost,
 * for the sake of displaying them in a damage indicator (element glyph with a number).
 */
class DamageIndicatorHistory {

	private val entries = mutableListOf<Entry>()

	/**
	 * Adds a damage indicator entry for the *attacker/caster* of a move/attack.
	 * The `BattleState` class should only invoke this on the `attacker.renderInfo.indicatorHistory`
	 */
	fun addAttackerIndicator(result: MoveResult, time: Time) {
		if (result.restoreAttackerHealth != 0) {
			entries.add(Entry(
				element = result.element,
				amount = abs(result.restoreAttackerHealth),
				type = if (result.restoreAttackerHealth > 0) ResultType.GainHealth else ResultType.LoseHealth,
				insertionTime = time,
				overrideColor = 0,
			))
		}

		if (result.restoreAttackerMana != 0) {
			entries.add(Entry(
				element = result.element,
				amount = abs(result.restoreAttackerMana),
				type = if (result.restoreAttackerMana > 0) ResultType.GainMana else ResultType.LoseMana,
				insertionTime = time,
				overrideColor = 0,
			))
		}
	}

	/**
	 * Adds a damage indicator entry for a *target* of a move/attack.
	 * The `BattleState` class should only invoke this on the `target.renderInfo.indicatorHistory`
	 */
	fun addTargetIndicator(result: MoveResult.Entry, shouldShowHealth: Boolean, time: Time) {
		if (result.missed) {
			entries.add(Entry(
				null, 0, ResultType.Miss,
				time, 0,
			))
		} else {
			if (shouldShowHealth) {
				val type = if (result.damage < 0) ResultType.GainHealth else ResultType.LoseHealth
				entries.add(Entry(
					result.element, abs(result.damage), type,
					time, result.overrideBlinkColor
				))
			}

			if (result.damageMana != 0) {
				val type = if (result.damageMana > 0) ResultType.LoseMana else ResultType.GainMana
				entries.add(Entry(
					result.element, abs(result.damageMana), type,
					time, 0,
				))
			}
		}
	}

	/**
	 * Adds a damage indicator entry for a combatant that takes on-turn damage/healing (e.g. Poison or Regen)
	 */
	fun addOnTurnIndicator(dpt: StatusEffect.TurnDamage, lostHealth: Int, time: Time) {
		entries.add(Entry(
			element = dpt.element,
			amount = abs(lostHealth),
			type = if (lostHealth > 0) ResultType.LoseHealth else ResultType.GainHealth,
			insertionTime = time,
			overrideColor = dpt.blinkColor,
		))
	}

	/**
	 * This method should be called on the `renderInfo.levelUpHistory` when a player levels-up
	 */
	fun addLevelUp(newLevel: Int, time: Time) {
		entries.add(Entry(
			element = null,
			amount = newLevel,
			type = ResultType.LevelUp,
			insertionTime = time,
			overrideColor = 0,
		))
	}

	/**
	 * Gets the most recent time instant where the combatant took damage.
	 *
	 * This is used for playing/timing e.g. the death & hurt animations
	 */
	fun mostRecentDamageTakenAt(timing: RenderTiming) = get(timing).filter {
		it.type == ResultType.LoseHealth || it.type == ResultType.LoseMana
	}.map { it.insertionTime }.lastOrNull()

	/**
	 * Gets the damage indicators that should be shown when rendering at the given timing
	 */
	fun get(timing: RenderTiming): List<Result> {
		if (entries.isEmpty() || entries[0].insertionTime.virtual > timing.now().virtual) return emptyList()

		val result = mutableListOf<Result>()
		var earliestNextIndicator = Duration.ZERO

		for (entry in entries) {
			val startTime = max(entry.insertionTime.virtual, earliestNextIndicator)
			val elapsedSinceStart = timing.now().virtual - startTime
			if (elapsedSinceStart < Duration.ZERO) break

			if (elapsedSinceStart < LIFETIME) {
				val relativeY = if (elapsedSinceStart < JUMP_DURATION) {
					1f - 2f * abs(0.5f - (elapsedSinceStart / JUMP_DURATION).toFloat())
				} else if (elapsedSinceStart < JUMP_DURATION * 2) {
					0.5f - abs(0.5f - ((elapsedSinceStart - JUMP_DURATION) / JUMP_DURATION).toFloat())
				} else 0f

				val heightFactor = if (elapsedSinceStart < START_FADING) 1f
				else 1f - 0.5f * ((elapsedSinceStart - START_FADING) / FADE_OUT_DURATION).toFloat()

				val opacity = if (elapsedSinceStart < FADE_IN_DURATION) (elapsedSinceStart / FADE_IN_DURATION).toFloat()
				else if (elapsedSinceStart > START_FADING) {
					(1f - ((elapsedSinceStart - START_FADING) / FADE_OUT_DURATION).toFloat()).pow(3)
				} else 1f

				val blinkColor = if (entry.element != null && entry.amount != 0) {
					if (entry.overrideColor == 0) entry.element.damageColor else entry.overrideColor
				} else 0

				val blinkIntensity = if (elapsedSinceStart < BLINK_DURATION / 3) {
					(elapsedSinceStart / (BLINK_DURATION / 3)).toFloat()
				} else if (elapsedSinceStart < BLINK_DURATION * 2 / 3) {
					1f
				} else max(0f, 1f - ((elapsedSinceStart - (BLINK_DURATION * 2 / 3)) / (BLINK_DURATION / 3)).toFloat())

				result.add(Result(
					entry.element,
					entry.amount,
					entry.type,
					entry.insertionTime,

					blinkColor,
					blinkIntensity,
					relativeY,
					heightFactor,
					opacity,
				))
			}

			earliestNextIndicator = startTime + MIN_DELAY
		}

		if (result.isEmpty()) entries.clear()
		return result
	}

	private class Entry(
		val element: Element?,
		val amount: Int,
		val type: ResultType,
		val insertionTime: Time,
		val overrideColor: Int,
	)

	/**
	 * This class is used in the return type of [get]:
	 * it represents a single damage indicator entry that should be rendered.
	 */
	class Result(

		/**
		 * The element glyph to be rendered, or `null` when no element glyph should be rendered
		 * (e.g. when an attack missed)
		 */
		val element: Element?,

		/**
		 * The amount of damage that should be shown. This field is ignored when `element == null`.
		 */
		val amount: Int,

		/**
		 * The type of damage/healing (e.g. [ResultType.LoseHealth])
		 */
		val type: ResultType,

		/**
		 * The 'insertion time' of the damage entry, which is the campaign time at which the damage/healing was taken
		 */
		val insertionTime: Time,

		/**
		 * The 'blink color' that the combatant should get (e.g. combatants will blink red upon taking damage)
		 */
		val blinkColor: Int,

		/**
		 * The intensity of [blinkColor], which is a number between 0 and 1:
		 * - 0 indicates that the blink color is ignored
		 * - 0.5 indicates that the combatant color is mixed 50-50 with the blink color
		 * - 1 indicates that the combatant color is completely replaced with the blink color
		 */
		val blinkIntensity: Float,

		/**
		 * After a damage indicator appears, it will 'jump' a bit.
		 *
		 * This field is used to indicate how 'high' the indicator should be rendered.
		 * It is a number between 0 and 1:
		 * - 0 means that the indicator should be rendered at its lowest point
		 * - 0.5 means that the indicator is in the 'middle' of its jump, and should be rendered between its highest
		 *   and lowest point
		 * - 1 means that the indicator is at the 'peak' of its jump, and should be rendered at its highest point
		 */
		val relativeY: Float,

		/**
		 * When the damage indicator is about to fade out, it will also change its shape: it will become less tall,
		 * but also wider.
		 *
		 * This field should be a number between 0 and 1:
		 * - 0 means that the height has become 0, which means that it has faded out completely
		 * - 0.5 means that the glyph and text should be rendered at 50% of its original height
		 * - 1 means that the width and height are still the original width and height
		 */
		val heightFactor: Float,

		/**
		 * The opacity of the indicator, which should be 1.0 most of the time. However, it will be smaller than 1.0
		 * during the fade-in and the fade-out.
		 */
		val opacity: Float,
	) {
		init {
			if (element == null && type != ResultType.Miss && type != ResultType.LevelUp) {
				throw IllegalArgumentException("Element is required for any type except Miss and LevelUp")
			}
			if (element != null && (type == ResultType.Miss || type == ResultType.LevelUp)) {
				throw IllegalArgumentException("Element must be null for type Miss and LevelUp")
			}
		}
	}

	/**
	 * The type of [Result.type], e.g. [LoseHealth] or [GainMana]
	 */
	enum class ResultType {

		/**
		 * An attack against this combatant missed
		 */
		Miss,

		/**
		 * A player just leveled-up
		 */
		LevelUp,

		/**
		 * This combatant recently lost health
		 */
		LoseHealth,

		/**
		 * This combatant recently regained health
		 */
		GainHealth,

		/**
		 * This combatant recently lost mana
		 */
		LoseMana,

		/**
		 * This combatant recently regained mana
		 */
		GainMana,
	}

	companion object {

		/**
		 * The minimum delay between the spawn of two damage indicators.
		 * If one attack causes 2 damage indicators on the same target,
		 * the 'second' one will appear half a second later than the 'first' indicator.
		 */
		val MIN_DELAY = 500.milliseconds

		/**
		 * The fade-in duration of damage indicators.
		 *
		 * The fade-in starts at the same time as the jump
		 */
		val FADE_IN_DURATION = 100.milliseconds

		/**
		 * The duration of the two 'jumps' of the damage indicator right after appearing
		 */
		val JUMP_DURATION = 250.milliseconds

		/**
		 * The duration of the 'damage blink' when a combatant takes damage.
		 *
		 * It reaches the maximum intensity after `BLINK_DURATION / 2`, and is completely gone after `BLINK_DURATION`
		 */
		val BLINK_DURATION = 700.milliseconds

		/**
		 * After the initial 'jumps', the indicator becomes fully visible/stable for 750 milliseconds
		 */
		val STABLE_DURATION = 500.milliseconds

		/**
		 * Once the stable duration is over, the indicator fades and 'squashes' in 250 milliseconds
		 */
		val FADE_OUT_DURATION = 250.milliseconds

		/**
		 * The indicator will start fading/squashing 1 second after appearing
		 */
		val START_FADING = JUMP_DURATION * 2 + STABLE_DURATION

		/**
		 * The lifetime of a damage indicator: they become invisible 1.25 seconds after they appear
		 */
		val LIFETIME = START_FADING + FADE_OUT_DURATION
	}
}
