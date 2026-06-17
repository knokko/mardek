package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.ActionEndOfChapter
import mardek.content.action.ActionNode
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionSetBackground
import mardek.content.action.ActionSetMusic
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionShowChapterName
import mardek.content.action.ActionTalk
import mardek.content.action.ActionToArea
import mardek.content.action.FixedAction
import mardek.content.action.FixedActionNode
import mardek.content.battle.BattleBackground
import mardek.input.Event
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.MouseMoveEvent
import mardek.state.ingame.CampaignState
import mardek.state.ingame.CampaignStateMachine
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.longArrayOf
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit

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
) : CampaignStateMachine() {

	/**
	 * When non-null, this music track should be played.
	 *
	 * This field is initially null, but can be changed by [ActionSetMusic].
	 */
	@BitField(id = 1, optional = true)
	var currentMusic: String? = null
		private set

	/**
	 * When non-null, this background will be displayed behind potential dialogues.
	 *
	 * This field is initially null, but can be changed by [ActionSetBackground].
	 */
	@BitField(id = 2, optional = true)
	@ReferenceField(stable = true, label = "battle backgrounds")
	var currentBackground: BattleBackground? = null
		private set

	/**
	 * The current overlay color, which should be rendered on top of the area and potential dialogues. This is
	 * initially 0 (fully transparent), but can be changed by [ActionSetOverlayColor].
	 *
	 * If the current action is an [ActionSetOverlayColor], this field will store the *old* overlay color. It will
	 * only be updated once the action is completed.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, commonValues = [0])
	var overlayColor: Int = 0

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
	var cutsceneSubtitle = Pair(1, "")

	/**
	 * When inside a dialogue, this is the number of characters (glyphs) that should be rendered in the box. When a
	 * dialogue node is reached, `shownDialogueCharacters` starts at 0, and slowly increases. Pressing 'E' or 'Q' speeds
	 * it up dramatically.
	 */
	var shownDialogueCharacters = 0f
		private set

	/**
	 * Whether the chat log should be shown during talk/dialogue actions.
	 * The player can toggle this by pressing the L key.
	 */
	var showChatLog = false

	private var speedUpShowingCharacters = false

	private var passedCutsceneTime = Duration.ZERO

	/**
	 * When the action of the current node is an [ActionEndOfChapter], this field will track the interaction state with
	 * the End of Chapter X screen.
	 */
	var endOfChapterState: EndOfChapterState? = null
		private set

	/**
	 * Used by the renderer to track the lightning effect renderer
	 */
	var lightningRenderInfo: Any = Object()

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
			if (action is ActionPlayCutscene) action.cutscene.payload.get()
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
		this.cutsceneSubtitle = Pair(1, "")
		this.passedCutsceneTime = Duration.ZERO
		this.speedUpShowingCharacters = false
		this.shownDialogueCharacters = 0f
	}

	private fun isAnimationAction(action: FixedAction) = action is ActionShowChapterName || action is ActionPlayCutscene

	private fun updateTalking(currentAction: ActionTalk, context: CampaignState.UpdateContext): Boolean {
		var speedModifier = 1f
		if (speedUpShowingCharacters) speedModifier = 80f
		if (context.input.isPressed(InputKey.Cancel)) speedModifier = 80f

		val appearanceSpeed = context.timeStep.toDouble(DurationUnit.SECONDS).toFloat() * speedModifier * 50f
		shownDialogueCharacters = min(
			shownDialogueCharacters + appearanceSpeed,
			currentAction.text.length.toFloat(),
		)

		return context.input.isPressed(InputKey.Cancel) && shownDialogueCharacters >= currentAction.text.length
	}

	/**
	 * The `CampaignState` should call this method during each of its own `update()`s
	 */
	override fun update(campaignContext: CampaignState.UpdateContext, campaign: CampaignState) {
		while (true) {
			val currentNode = this.node
			if (currentNode is FixedActionNode) {
				val action = currentNode.action
				var shouldGoToNextNode = false

				if (isAnimationAction(action) && this.finishedAnimationNode) shouldGoToNextNode = true

				if (action is ActionPlayCutscene) {
					val oldPassedTime = passedCutsceneTime
					passedCutsceneTime += campaignContext.timeStep

					for (potentialSound in action.cutscene.sounds) {
						if (potentialSound.delay > oldPassedTime && potentialSound.delay <= passedCutsceneTime) {
							campaignContext.soundQueue.insert(potentialSound.sound)
						}
					}
				}

				if (action is ActionTalk) {
					shouldGoToNextNode = updateTalking(action, campaignContext)
					if (shouldGoToNextNode) {
						campaign.addToChatLog(ChatLogEntry(
							speaker = action.speaker.getDisplayName(null, campaign.party) ?: "no-one?",
							speakerElement = action.speaker.getElement(null, campaign.party),
							text = action.text,
						))
					}
				}

				if (action is ActionSetMusic) {
					this.currentMusic = action.newMusicTrack
					shouldGoToNextNode = true
				}

				if (action is ActionSetBackground) {
					this.currentBackground = action.newBackground
					shouldGoToNextNode = true
				}

				if (action is ActionSetOverlayColor) {
					val finishTime = currentNodeStartTime + action.transitionTime.inWholeNanoseconds
					shouldGoToNextNode = System.nanoTime() >= finishTime
				}

				if (action is ActionEndOfChapter && endOfChapterState != null) {
					endOfChapterState!!.update(campaignContext)
					shouldGoToNextNode = endOfChapterState!!.shouldContinue
				}

				if (shouldGoToNextNode) {
					val nextNode = currentNode.next ?: throw IllegalArgumentException(
						"${currentNode.action} must have a next node"
					)
					toNextNode(nextNode)
					if (action is ActionSetOverlayColor) this.overlayColor = action.color
					continue
				}
			}

			break
		}

		val finalNode = node
		if (finalNode is FixedActionNode && finalNode.action is ActionEndOfChapter) {
			if (endOfChapterState == null) endOfChapterState = EndOfChapterState()
		} else endOfChapterState = null

		maybeGoToAreaState(campaign)
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

	private fun processKeyPress(context: CampaignState.UpdateContext, campaign: CampaignState, key: InputKey) {
		val currentNode = this.node
		if (currentNode is FixedActionNode) {
			val currentAction = currentNode.action
			val isSkipKey = key == InputKey.Interact || key == InputKey.Cancel || key == InputKey.Escape ||
					key == InputKey.ToggleMenu

			var goToNextNode = false
			if (isSkipKey && isAnimationAction(currentAction)) goToNextNode = true

			if (currentAction is ActionTalk) {
				if (key == InputKey.Interact) {
					if (shownDialogueCharacters >= currentAction.text.length) {
						goToNextNode = true
						campaign.addToChatLog(ChatLogEntry(
							speaker = currentAction.speaker.getDisplayName(null, campaign.party) ?: "no-one?",
							speakerElement = currentAction.speaker.getElement(null, campaign.party),
							text = currentAction.text,
						))
					} else speedUpShowingCharacters = true
				}
				if (key == InputKey.ToggleChatLog) showChatLog = !showChatLog
			}

			if (currentAction is ActionEndOfChapter && endOfChapterState != null) {
				endOfChapterState!!.processKeyPress(context, campaign, key)
			}

			if (goToNextNode) {
				val nextNode = currentNode.next ?: throw IllegalArgumentException(
					"${currentNode.action} must have a non-null next node"
				)
				toNextNode(nextNode)
			}
		}
	}

	private fun processMouseMove(campaign: CampaignState, event: MouseMoveEvent) {
		val currentNode = this.node
		if (currentNode is FixedActionNode) {
			val currentAction = currentNode.action

			if (currentAction is ActionEndOfChapter && endOfChapterState != null) {
				endOfChapterState!!.processMouseMove(campaign, event)
			}
		}
	}

	private fun maybeGoToAreaState(campaign: CampaignState) {
		node.let {
			if (it is FixedActionNode && it.action is ActionToArea) {
				val action = it.action as ActionToArea
				val nextState = AreaState(
					action.area, campaign.story, campaign.expressionContext(),
					AreaPosition(action.x, action.y),
					action.direction,
				)
				campaign.state = nextState
				if (it.next != null) {
					nextState.suspension = AreaSuspensionActions(
						AreaActionsState(it.next, null)
					)
				}
			}
		}
	}

	override fun processEvent(
		event: Event,
		campaignContext: CampaignState.UpdateContext,
		campaign: CampaignState
	) {
		if (event is InputKeyEvent && event.didPress) processKeyPress(campaignContext, campaign, event.key)
		if (event is MouseMoveEvent) processMouseMove(campaign, event)
		maybeGoToAreaState(campaign)
	}
}
