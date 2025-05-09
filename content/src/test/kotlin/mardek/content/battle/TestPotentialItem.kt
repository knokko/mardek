package mardek.content.battle

import mardek.content.inventory.Item
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestPotentialItem {

	@Test
	fun testEmptyPotentialEquipment() {
		repeat(10_000) {
			assertNull(PotentialEquipment.EMPTY.pick())
		}
	}

	@Test
	fun testNonNullEquipment() {
		val sword = Item()
		val mace = Item()
		val potential = PotentialEquipment(arrayListOf(
			PotentialItem(sword, 60),
			PotentialItem(mace, 40)
		))
		repeat(10_000) {
			val next = potential.pick()
			assertTrue(next === sword || next === mace)
		}
	}

	@Test
	fun testNonEmptyPotentialEquipment() {
		var noneCounter = 0
		var maceCounter = 0
		var swordCounter = 0

		val sword = Item()
		val mace = Item()
		val potential = PotentialEquipment(arrayListOf(
			PotentialItem(null, 50),
			PotentialItem(sword, 10),
			PotentialItem(mace, 40)
		))

		repeat(100_000) {
			val next = potential.pick()
			assertTrue(next == null || next === mace || next === sword)

			if (next == null) noneCounter += 1
			if (next === mace) maceCounter += 1
			if (next === sword) swordCounter += 1
		}

		assertTrue(noneCounter in 45000..55000, "Expected $noneCounter to be in [48000, 52000]")
		assertTrue(swordCounter in 5000..15000, "Expected $swordCounter to be in [7000, 13000]")
		assertTrue(maceCounter in 35000..45000, "Expected $maceCounter to be in [38000, 42000]")
	}
}
