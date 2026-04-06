package mardek.state.ingame.battle

import mardek.content.stats.StatusEffect
import kotlin.math.abs

private const val DURATION = 1000_000_000L

/**
 * This is the type of [CombatantRenderInfo.effectHistory]. It tracks which status effects the combatant recently
 * gained and lost, which is needed by the renderer.
 */
class StatusEffectHistory {

	private val entries = mutableListOf<Entry>()

	/**
	 * This method should be called when the combatant gets a new status effect when `System.nanoTime() == time`.
	 */
	fun add(effect: StatusEffect, time: Long) {
		entries.add(Entry(time, effect, Type.Add))
	}

	/**
	 * This method should be called when the combatant loses a status effect when `System.nanoTime() == time`.
	 */
	fun remove(effect: StatusEffect, time: Long) {
		entries.add(Entry(time, effect, Type.Remove))
	}

	/**
	 * Gets the status effect 'mutation' that should be shown when `System.nanoTime() == time`, or `null` when the
	 * combatant didn't gain or lose any status effects recently.
	 */
	fun get(time: Long): Current? {
		val iterator = entries.iterator()
		while (iterator.hasNext()) {
			val entry = iterator.next()
			if (entry.startTime < entry.insertionTime) entry.startTime = time
			val passedTime = time - entry.startTime
			if (passedTime < DURATION) return Current(
				entry.effect, entry.type, passedTime.toFloat() / DURATION
			)
			iterator.remove()
		}
		return null
	}

	/**
	 * This is the return type of [get]. It represents either the addition or removal of a status effect from the
	 * combatant. When a frame is rendered at time `t`, the renderer should render the mutation returned by `get(t)`.
	 */
	class Current(

		/**
		 * The status effect that was added or removed
		 */
		val effect: StatusEffect,

		/**
		 * The mutation type (either [Type.Add] or [Type.Remove])
		 */
		val type: Type,

		/**
		 * When a status effect is added/removed, a very short animation is rendered to indicate this. The
		 * `relativeTime` indicates how 'far' this animation should be:
		 * - `0f` means that the animation is about to start
		 * - `1f` means that the animation is about to end
		 * - `0.5f` means that the animation is halfway
		 * - etc...
		 */
		val relativeTime: Float,
	) {
		override fun equals(other: Any?) = other is Current && effect === other.effect
				&& type == other.type && abs(relativeTime - other.relativeTime) < 0.001f

		override fun hashCode() = effect.hashCode() + 3 * type.hashCode() - relativeTime.hashCode()

		override fun toString() = "$type $effect progress $relativeTime"
	}

	private class Entry(val insertionTime: Long, val effect: StatusEffect, val type: Type) {
		var startTime = insertionTime - 1L
	}

	/**
	 * The possible mutation types [Current.type]: either [Type.Add] or [Type.Remove]
	 */
	enum class Type {

		/**
		 * A status effect was recently given to the combatant
		 */
		Add,

		/**
		 * A status effect was recently removed from the combatant
		 */
		Remove
	}
}
