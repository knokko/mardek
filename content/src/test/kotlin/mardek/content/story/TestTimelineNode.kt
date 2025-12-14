package mardek.content.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.UUID

internal fun simpleNode(name: String, children: Array<TimelineNode>, isAbstract: Boolean = false) = TimelineNode(
	id = UUID.randomUUID(),
	name = name,
	children = children,
	variables = emptyArray(),
	isAbstract = isAbstract,
)

class TestTimelineNode {

	@Test
	fun testDiscoverParentsOfChildrenSimple() {
		val childBAA = simpleNode("baa", emptyArray())
		val childBA = simpleNode("ba", arrayOf(childBAA))
		val childBB = simpleNode("bb", emptyArray())
		val childA = simpleNode("a", emptyArray())
		val childB = simpleNode("b", arrayOf(childBA, childBB))
		val childC = simpleNode("c", emptyArray())
		val root = simpleNode("root", arrayOf(childA, childB, childC))
		root.discoverParentsOfChildren()

		assertNull(root.parent)

		assertEquals(0, childA.parentIndex)
		assertSame(root, childA.parent)

		assertEquals(1, childB.parentIndex)
		assertSame(root, childB.parent)

		assertEquals(2, childC.parentIndex)
		assertSame(root, childC.parent)

		assertEquals(0, childBA.parentIndex)
		assertSame(childB, childBA.parent)

		assertEquals(1, childBB.parentIndex)
		assertSame(childB, childBB.parent)

		assertEquals(0, childBAA.parentIndex)
		assertSame(childBA, childBAA.parent)
	}

	@Test
	fun testDiscoverParentsOfChildrenVeryDeep() {
		val allNodes = mutableListOf<TimelineNode>()

		var previousNode = emptyArray<TimelineNode>()
		repeat(100_000) {
			val node = simpleNode("node$it", previousNode)
			previousNode = arrayOf(node)
			allNodes.add(node)
		}

		val root = allNodes.last()
		root.discoverParentsOfChildren()
		assertNull(root.parent)

		assertEquals(100_000, allNodes.size)
		for (index in 0 until allNodes.size - 1) {
			assertEquals(0, allNodes[index].parentIndex)
			assertSame(allNodes[index + 1], allNodes[index].parent)
		}
	}

	@Test
	fun testNonAbstractDescendantSimple() {
		val childABA = simpleNode("aba", emptyArray())
		val childAA = simpleNode("aa", emptyArray())
		val childAB = simpleNode("ab", arrayOf(childABA))
		val childA = simpleNode("a", arrayOf(childAA, childAB), isAbstract = true)
		val root = simpleNode("root", arrayOf(childA))

		assertSame(root, root.nonAbstractDescendant())
		assertSame(childAA, childA.nonAbstractDescendant()) // Since childA is abstract
		assertSame(childAA, childAA.nonAbstractDescendant())
		assertSame(childAB, childAB.nonAbstractDescendant())
		assertSame(childABA, childABA.nonAbstractDescendant())
	}

	@Test
	fun testNonAbstractDescendantDeep() {
		val leaf = simpleNode("leaf", emptyArray())

		val beforeLeaf = mutableListOf<TimelineNode>()
		var node = leaf
		repeat(2000) {
			node = simpleNode("before-leaf$it", arrayOf(node), isAbstract = true)
			beforeLeaf.add(node)
		}

		val middle = simpleNode("middle", arrayOf(node))

		val beforeMiddle = mutableListOf<TimelineNode>()
		node = middle
		repeat(2000) {
			node = simpleNode("before-middle$it", arrayOf(node), isAbstract = true)
			beforeMiddle.add(node)
		}

		val root = simpleNode("root", arrayOf(node))
		root.discoverParentsOfChildren()

		assertSame(root, root.nonAbstractDescendant())
		assertSame(middle, root.children[0].nonAbstractDescendant())
		assertSame(middle, beforeMiddle[1000].nonAbstractDescendant())
		assertSame(middle, middle.nonAbstractDescendant())
		assertSame(leaf, middle.children[0].nonAbstractDescendant())
		assertSame(leaf, beforeLeaf[1234].nonAbstractDescendant())
		assertSame(leaf, beforeLeaf.last().nonAbstractDescendant())
		assertSame(leaf, leaf.nonAbstractDescendant())
	}
}
