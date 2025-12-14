package mardek.content.story

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TestTimeline {

	@Test
	fun testIsAfterInvalid() {
		val timeline = Timeline(
			id = UUID.randomUUID(),
			name = "correct",
			root = simpleNode("root", emptyArray()),
			needsActivation = false
		)
		val otherTimeline = Timeline(
			id = UUID.randomUUID(),
			name = "other",
			root = simpleNode("other root", emptyArray()),
			needsActivation = false
		)

		assertThrows<IllegalArgumentException> {
			timeline.isAfter(timeline.root, otherTimeline.root)
		}
		assertThrows<IllegalArgumentException> {
			timeline.isAfter(otherTimeline.root, timeline.root)
		}
		assertFalse(timeline.isAfter(timeline.root, timeline.root))
		assertFalse(otherTimeline.isAfter(otherTimeline.root, otherTimeline.root))
	}

	@Test
	fun testIsAfter() {
		val childAA = simpleNode("AA", emptyArray())
		val childAB = simpleNode("AB", emptyArray())
		val childA = simpleNode("A", arrayOf(childAA, childAB), isAbstract = true)

		val childBA = simpleNode("BA", emptyArray())
		val childB = simpleNode("B", arrayOf(childBA), isAbstract = true)

		val childC = simpleNode("C", emptyArray())

		val root = simpleNode("root", arrayOf(childA, childB, childC))
		val timeline = Timeline(
			id = UUID.randomUUID(),
			name = "timeline",
			root = root,
			needsActivation = true,
		)

		fun assertAfter(left: TimelineNode, right: TimelineNode) {
			assertTrue(timeline.isAfter(left, right))
			assertFalse(timeline.isAfter(right, left))
		}

		fun assertSamePlace(left: TimelineNode, right: TimelineNode) {
			assertFalse(timeline.isAfter(left, right))
			assertFalse(timeline.isAfter(right, left))
		}

		assertAfter(childA, root)
		assertAfter(childAB, root)
		assertSamePlace(childAA, childA) // childA is abstract, so it is equivalent to childAA
		assertAfter(childAB, childAA)
		assertSamePlace(childAA, childAA)

		assertAfter(childB, childA)
		assertAfter(childB, childAB)
		assertAfter(childB, root)
		assertSamePlace(childB, childB)
		assertSamePlace(childBA, childB)

		assertAfter(childC, childB)
		assertAfter(childC, childBA)
		assertAfter(childC, childAB)
		assertAfter(childC, root)
		assertSamePlace(childC, childC)

		assertSamePlace(root, root)
	}
}
