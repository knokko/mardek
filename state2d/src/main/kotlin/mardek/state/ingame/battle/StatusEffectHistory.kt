package mardek.state.ingame.battle

import mardek.content.stats.StatusEffect
import kotlin.math.abs

private const val DURATION = 1000_000_000L

class StatusEffectHistory {

	private val entries = mutableListOf<Entry>()

	fun add(effect: StatusEffect, time: Long) {
		entries.add(Entry(time, effect, Type.Add))
	}

	fun remove(effect: StatusEffect, time: Long) {
		entries.add(Entry(time, effect, Type.Remove))
	}

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

	class Current(val effect: StatusEffect, val type: Type, val relativeTime: Float) {
		override fun equals(other: Any?) = other is Current && effect === other.effect
				&& type == other.type && abs(relativeTime - other.relativeTime) < 0.001f

		override fun hashCode() = effect.hashCode() + 3 * type.hashCode() - relativeTime.hashCode()

		override fun toString() = "$type $effect progress $relativeTime"
	}

	private class Entry(val insertionTime: Long, val effect: StatusEffect, val type: Type) {
		var startTime = insertionTime - 1L
	}

	enum class Type {
		Add,
		Remove
	}
}
