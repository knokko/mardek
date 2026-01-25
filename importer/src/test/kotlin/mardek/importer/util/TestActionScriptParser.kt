package mardek.importer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.ArrayList

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

	@Test
	fun julioRegressionTest() {
		val actual = parseActionScriptNestedList("[[\"\",\"\\\"Oh, Julio!\\\" moaned Cynthia as she caressed his rippling muscles slenderly. \\\"I love you! I love you, I love you, I love you!\\\"\"],[\"\",\"\\\"But... this is not right. This cannot be.\\\" They both knew that their fathers would be appalled, yet they did not give but a damn. \\\"I\\'ll show you something else that cannot be!\\\" grunted Julio sexily, eyes blazing with fiery passion, as he dropped his fashionable pantaloons. \\\"My gosh! It\\'s so-\"],[\"\",\"(Ahem. Not your sort of thing, really.)\"]]")
		assertEquals(arrayListOf("\"\"", "\"(Ahem. Not your sort of thing, really.)\""), (actual as ArrayList<*>)[2])
	}
}
