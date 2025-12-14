package mardek.content.story

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID
import kotlin.math.min

/**
 * Represents one of the parallel timelines of the `StoryContent`.
 *
 * Each timeline has a `root` node, which normally has multiple descending child nodes. For each timeline, the
 * game state (`StoryState`) must be inside exactly 1 of its nodes. The states of all timelines together define the
 * overall state of the story.
 *
 * The story has 1 *main* timeline, as well as several timelines for e.g. quests that can run parallel to the main
 * story.
 */
@BitStruct(backwardCompatible = true)
class Timeline(
	/**
	 * The unique ID of this timeline, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of this timeline, which is only useful for debugging and editing
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The root node of this timeline
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "timeline nodes")
	val root: TimelineNode,

	/**
	 * Whether this timeline needs to be activated by the node of another timeline. When `true`, the `variables` of
	 * the nodes of this timeline are ignored, unless this timeline is activated by the `activatesTimeline`
	 * of an active `TimelineNode`. Note that this restriction does not apply to nodes with
	 * `ignoresTimelineActivation = true`.
	 *
	 * The `needsActivation` of the main story timeline is `false` (the main story is always active), but the
	 * `needsActivation` of most other timelines (e.g. quest timelines) is `true`.
	 */
	@BitField(id = 3)
	val needsActivation: Boolean,
) : BitPostInit {

	init {
		root.discoverParentsOfChildren()
	}

	internal constructor() : this(UUID.randomUUID(), "", TimelineNode(), false)

	override fun toString() = "Timeline(name=$name)"

	override fun postInit(context: BitPostInit.Context) {
		root.discoverParentsOfChildren()
	}

	/**
	 * Determines whether `left` occurs *later* in this timeline than `right`.
	 *
	 * Both `left` and `right` must be nodes of this timeline. If not, an `IllegalArgumentException` will be thrown.
	 */
	fun isAfter(left: TimelineNode, right: TimelineNode): Boolean {
		val leftIndices = getIndices(left.nonAbstractDescendant())
		val rightIndices = getIndices(right.nonAbstractDescendant())

		for (indirectIndex in 0 until min(leftIndices.size, rightIndices.size)) {
			if (leftIndices[indirectIndex] > rightIndices[indirectIndex]) return true
			if (leftIndices[indirectIndex] < rightIndices[indirectIndex]) return false
		}

		return leftIndices.size > rightIndices.size
	}

	private fun getIndices(node: TimelineNode): IntArray {
		val indices = mutableListOf<Int>()
		var nextNode = node
		while (nextNode.parent != null) {
			indices.add(nextNode.parentIndex)
			nextNode = nextNode.parent!!
		}

		if (nextNode !== root) throw IllegalArgumentException("Node $node does not occur on timeline $this")
		return indices.toIntArray().reversedArray()
	}
}
