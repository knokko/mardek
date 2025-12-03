package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.ActionNode
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionShowChapterName
import mardek.content.action.FixedAction
import mardek.content.action.FixedActionNode
import mardek.input.InputKey

/**
 * If a campaign action sequence is currently in progress, the `actions` field of the current `CampaignState` will be
 * an instance of this class. This class tracks the state/progress of an action sequence.
 *
 * This class is the campaign-level counterpart of `AreaActionsState`: this class will be used when the player is
 * currently **not** inside an area (e.g. the start of a chapter), whereas `AreaActionsState` will be used when the
 * player is inside an area (99% of the time).
 */
@BitStruct(backwardCompatible = true)
class CampaignActionsState(

	/**
	 * The current node that is being executed, initially the root node of the action sequence.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "action nodes")
	var node: ActionNode,
) {

	/**
	 * The time at which the current `node` was activated/started: when the state transitions to a new node, this
	 * field is set to `System.nanoTime()`.
	 */
	var currentNodeStartTime = System.nanoTime()
		private set

	/**
	 * When the current node is an animation-based node, this field is used to track whether the renderer has finished
	 * rendering it.
	 *
	 * This field will be set to `false` at the start of each node. When the node is animation-based, the renderer will
	 * set it to `true` after it has finished rendering the entire animation.
	 */
	var finishedAnimationNode = false

	/**
	 * Some cutscenes (chapter 1 intro) have subtitles. This field tracks the current subtitle that should be rendered.
	 * The renderer is responsible for writing to this field.
	 */
	var cutsceneSubtitle = ""

	@Suppress("unused")
	private constructor() : this(FixedActionNode())

	override fun toString() = "CampaignActions($node)"

	/**
	 * By calling `action.cutscene.get()`, we will deserialize the cutscene on *this* thread, which may block this
	 * thread for a second on slow old laptops. If we would not do this, the render thread may deserialize the cutscene,
	 * which would block the render thread, which is undesirable...
	 */
	private fun makeSureRenderThreadDoesNotGetBlocked(next: ActionNode) {
		if (next is FixedActionNode) {
			val action = next.action
			if (action is ActionPlayCutscene) action.cutscene.get()
		}
	}

	/**
	 * Transitions to the next node, and sets `currentNodeStartTime` to the current time
	 */
	private fun toNextNode(next: ActionNode) {
		makeSureRenderThreadDoesNotGetBlocked(next)
		this.node = next
		this.currentNodeStartTime = System.nanoTime()
		this.finishedAnimationNode = false
		this.cutsceneSubtitle = ""
	}

	private fun isAnimationAction(action: FixedAction) = action is ActionShowChapterName || action is ActionPlayCutscene

	/**
	 * The `CampaignState` should call this method during each of its own `update()`s
	 */
	fun update() {
		val currentNode = this.node
		if (currentNode is FixedActionNode) {
			if (isAnimationAction(currentNode.action) && this.finishedAnimationNode) toNextNode(currentNode.next ?:
					throw IllegalArgumentException("${currentNode.action} must have a next node")
			)
		}
	}

	/**
	 * This method should be called when the player enters the campaign state/session, so either:
	 * - when the player starts a new game, or
	 * - when the player loads a saved game
	 *
	 * This method will reset the `currentNodeStartTime`, as well as some minor other stuff.
	 */
	fun markSessionStart() {
		toNextNode(this.node)
	}

	/**
	 * The `CampaignState` should propagate pressed keys whenever a key is pressed while a `CampaignActionsState` is
	 * active
	 */
	fun processKeyPress(key: InputKey) {
		val currentNode = this.node
		if (currentNode is FixedActionNode) {
			val isSkipKey = key == InputKey.Interact || key == InputKey.Cancel || key == InputKey.Escape ||
					key == InputKey.ToggleMenu
			val canSkipAction = isAnimationAction(currentNode.action)
			if (isSkipKey && canSkipAction) toNextNode(currentNode.next ?: throw IllegalArgumentException(
				"${currentNode.action} must have a non-null next node"
			))
		}
	}
}
