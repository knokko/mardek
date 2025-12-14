package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.UUID

/**
 * Represents a possible node/state of a `Timeline`.
 *
 * At any point in time, the `StoryState` is in exactly 1 `TimelineNode` of each `Timeline`. Furthermore, at any point
 * in time, the value of each timeline variable is **derived** from the active node of each timeline. Most importantly,
 * the values of timeline variables are **not** stored in the save files: only the active node of each timeline.
 */
@BitStruct(backwardCompatible = true)
class TimelineNode(

	/**
	 * The unique ID of this node, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of this node, which is only useful for debugging and editing
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The child nodes of this timeline node. The story state typically transitions through each child node before
	 * leaving the parent node. When the story state is inside a child node, the `variables` of all parent/ancestor
	 * nodes are also activated.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "timeline nodes")
	val children: Array<TimelineNode>,

	/**
	 * The `TimelineVariable` assignments of this node. Whenever the story state is in this timeline node, each
	 * variable assignment in `variables` is activated. When the story state leaves this node, each variable
	 * assignment is removed, except those with `appliesToFutureNodes = true`.
	 *
	 * At any point in time, the current value of each timeline variable is desired from the active timeline nodes
	 * (and their parent nodes).
	 */
	@BitField(id = 3)
	val variables: Array<TimelineAssignment<*>>,

	/**
	 * The timelines that are *activated* when the timeline state is in this node.
	 *
	 * This array should only contain timelines with `needsActivation = true`, since timelines with
	 * `needsActivation = false` are always activated.
	 *
	 * This field is typically used to activate side-quest-related timelines that should be 'closed' automatically
	 * when the main story state moves on.
	 *
	 * For the majority of nodes, their `activatesTimelines` will be an empty array, since there are many more
	 * `TimelineNode`s than `Timeline`s.
	 */
	@BitField(id = 4)
	@ReferenceField(stable = false, label = "timelines")
	val activatesTimelines: Array<Timeline> = emptyArray(),

	/**
	 * When a node is abstract, the timeline state can **not** be in that node.
	 * Instead, when a timeline state reaches an abstract node, it will automatically transition to its first
	 * child node. When that child node is also abstract, it will go to the first child of that child node, etc...
	 *
	 * Abstract nodes must have at least 1 child node.
	 */
	@BitField(id = 5)
	val isAbstract: Boolean,

	/**
	 * When this node is part of a `Timeline` with `needsActivation = true`, the `variables` and `activatesTimelines`
	 * of this node will be ignored when its timeline is not *activated*.
	 *
	 * But, when `ignoresTimelineActivation = true`, the `variables` and `activatesTimelines` can be applied,
	 * regardless of whether its timeline is *activated*. Note however that the `StoryState` still needs to be in this
	 * node: if not, `variables` and `activatesTimelines` are ignored, regardless of `ignoresTimelineActivation`.
	 */
	@BitField(id = 6)
	val ignoresTimelineActivation: Boolean = false,
) {

	internal constructor() : this(
		UUID(0, 0), "", emptyArray(),
		emptyArray(), emptyArray(), false,
	)

	/**
	 * The parent node of this node:
	 * - when `this.parent != null`, then this node is `this.parent.children[parentIndex]`
	 * - when `this.parent == null`, then this node is the root node of a timeline
	 */
	var parent: TimelineNode? = null
		private set

	/**
	 * When `parent != null`, it must hold that `parent.children[parentIndex] == this`
	 */
	var parentIndex: Int = -1
		private set

	init {
		if (isAbstract && children.isEmpty()) throw IllegalArgumentException(
			"Invalid node $name: nodes must have at least 1 child"
		)
	}

	override fun toString() = "TimelineNode($name)"

	/**
	 * This method populates the `parent` field of each descendant of this timeline node. `Timeline`s call this method
	 * on their root node during their `postInit` method.
	 */
	internal fun discoverParentsOfChildren() {
		val remainingNodes = ArrayList<TimelineNode>()
		remainingNodes.addAll(this.children)
		for ((index, child) in this.children.withIndex()) {
			child.parent = this
			child.parentIndex = index
		}

		while (remainingNodes.isNotEmpty()) {
			val nextNode = remainingNodes.removeLast()
			remainingNodes.addAll(nextNode.children)
			for ((index, child) in nextNode.children.withIndex()) {
				child.parent = nextNode
				child.parentIndex = index
			}
		}
	}

	/**
	 * - If `this.isAbstract` is `false`, this method simply returns this node.
	 * - If `this.isAbstract` is `true`, this method returns the first descendant of this node whose `isAbstract` is
	 * `false`
	 */
	fun nonAbstractDescendant(): TimelineNode {
		var node = this
		while (node.isAbstract) node = node.children[0]
		return node
	}
}
