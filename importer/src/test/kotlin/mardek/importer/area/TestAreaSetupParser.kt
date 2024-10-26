package mardek.importer.area

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestAreaSetupParser {

	@Test
	fun testParseAreaSetup() {
		assertEquals(emptyMap<String, String>(), parseAreaSetup("{}"))
		assertEquals(mapOf(Pair("clearmap", "1")), parseAreaSetup("{clearmap:1}"))
		assertEquals(mapOf(
			Pair("WARP", "1"),
			Pair("clearmap", "1")
		), parseAreaSetup("{WARP:1,clearmap:1}"))
	}
}