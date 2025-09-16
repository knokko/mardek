package mardek.state.ingame.battle

import mardek.content.stats.StatusEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

private const val DURATION = 1000_000_000L

class TestStatusEffectHistory {

	@Test
	fun testEmptyHistory() {
		val history = StatusEffectHistory()
		assertNull(history.get(-10))
		assertNull(history.get(0))
		assertNull(history.get(100))
		assertNull(history.get(System.nanoTime()))
		assertNull(history.get(System.nanoTime()))
	}

	@Test
	fun testAddSingleEffect() {
		val poison = StatusEffect()
		val history = StatusEffectHistory()
		history.add(poison, -500)
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0f), history.get(4000)
		)
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0.5f),
			history.get(4000 + DURATION / 2)
		)
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 1f),
			history.get(4000 + DURATION - 1)
		)
		assertNull(history.get(4000 + DURATION))
	}

	@Test
	fun testChain() {
		val sleep = StatusEffect()
		val poison = StatusEffect()
		val history = StatusEffectHistory()
		history.add(sleep, 100)
		history.add(poison, 100)

		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 0f
		), history.get(100))
		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 1f
		), history.get(100 + DURATION - 1))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0f
		), history.get(100 + DURATION))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 1f
		), history.get(100 + 2 * DURATION - 1))
		assertNull(history.get(100 + 2 * DURATION))

		history.remove(sleep, 5 * DURATION)
		history.add(sleep, 5 * DURATION)

		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 0f
		), history.get(6 * DURATION))
		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 1f
		), history.get(7 * DURATION - 1))
		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 0f
		), history.get(8 * DURATION))
		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 1f
		), history.get(9 * DURATION - 1))
		assertNull(history.get(9 * DURATION))

		history.remove(sleep, 10 * DURATION)
		history.remove(poison, 10 * DURATION)
		history.add(poison, 11 * DURATION)

		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 0f
		), history.get(10 * DURATION))
		assertEquals(StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 1f
		), history.get(11 * DURATION - 1))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Remove, 0f
		), history.get(12 * DURATION))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Remove, 1f
		), history.get(13 * DURATION - 1))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0f
		), history.get(14 * DURATION))
		assertEquals(StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 1f
		), history.get(15 * DURATION - 1))
		assertNull(history.get(15 * DURATION))
	}
}
