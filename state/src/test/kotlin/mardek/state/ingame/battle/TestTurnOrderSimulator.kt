package mardek.state.ingame.battle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestTurnOrderSimulator {

	@Test
	fun testComputePriority() {
		assertEquals(12, TurnOrderEntry(MonsterCombatantState(), 12, 0, 1).computePriority())
		assertNull(TurnOrderEntry(MonsterCombatantState(), 12, 1, 1).computePriority())
		assertEquals(12, TurnOrderEntry(MonsterCombatantState(), 12, 0, 2).computePriority())
		assertEquals(6, TurnOrderEntry(MonsterCombatantState(), 12, 1, 2).computePriority())
	}

	@Test
	fun testSimulateWithHaste() {
		val enemy = MonsterCombatantState()
		val player = MonsterCombatantState()
		val simulator = TurnOrderSimulator(listOf(
			TurnOrderEntry(enemy, 25, 1, 2),
			TurnOrderEntry(player, 15, 0, 1)
		))

		// The enemy with haste has already spent 1 of its 2 turns this round
		assertFalse(simulator.checkReset())
		assertSame(player, simulator.next())
		assertFalse(simulator.checkReset())
		assertSame(enemy, simulator.next())

		// In the next round, the enemy can use both turns
		assertTrue(simulator.checkReset())
		assertSame(enemy, simulator.next())
		assertFalse(simulator.checkReset())
		assertSame(player, simulator.next())
		assertFalse(simulator.checkReset())
		assertSame(enemy, simulator.next())

		assertTrue(simulator.checkReset())
	}
}
