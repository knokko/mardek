package mardek.importer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestActionScriptParser {

	@Test
	fun testParseActionScriptNestedList() {
		assertEquals("123", parseActionScriptNestedList("123"))
		assertEquals(arrayListOf("\"m\"", "45"), parseActionScriptNestedList("[\"m\", 45]"))
		assertEquals(
			arrayListOf("\"SPECIAL\"", "\"SPR_CALC\"", "-2"),
			parseActionScriptNestedList("[\"SPECIAL\",\"SPR_CALC\",-2]")
		)
		assertEquals(
			arrayListOf("\"m\"", "1", "null", arrayListOf("\"crescendo\"", "0.25")),
			parseActionScriptNestedList("[\"m\",1,null,[\"crescendo\",0.25]]")
		)
		assertEquals(
			arrayListOf(
				"hi",
				arrayListOf("a", "b", arrayListOf("c"), arrayListOf<Any>()),
				"d",
				arrayListOf(arrayListOf("e"), "f")
			),
			parseActionScriptNestedList("[hi, [a, b, [c], []],d, [[e], f]]")
		)
	}

	@Test
	fun testParseEmptyActionScriptList() {
		assertEquals(0, parseActionScriptObjectList("[]").size)
	}

	@Test
	fun testParseEmptyObject() {
		assertEquals(0, parseActionScriptObject("{}").size)
	}
}
