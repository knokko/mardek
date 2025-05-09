package mardek.content.stats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestResistances {

	@Test
	fun testGetElementalResistance() {
		val water = Element()
		val fire = Element()
		val earth = Element()
		val resistances = Resistances(
			elements = arrayListOf(
				ElementalResistance(fire, 0.25f),
				ElementalResistance(water, -0.5f),
				ElementalResistance(fire, 0.5f)
			),
			effects = ArrayList(0)
		)

		assertEquals(0f, resistances.get(earth))
		assertEquals(0.75f, resistances.get(fire))
		assertEquals(-0.5f, resistances.get(water))
	}
}
