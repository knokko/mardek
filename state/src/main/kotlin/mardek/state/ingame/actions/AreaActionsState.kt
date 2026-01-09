package mardek.state.ingame.actions

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.*
import mardek.content.area.Direction
import mardek.content.area.objects.AreaCharacter
import mardek.content.characters.PlayableCharacter
import mardek.content.story.Timeline
import mardek.content.story.TimelineNode
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.FadingCharacter
import mardek.state.ingame.area.NextAreaPosition
import mardek.state.ingame.story.StoryState
import mardek.state.saves.SaveSelectionState
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * If an action sequence is currently in progress, the `actions` field of the current `AreaState` will be an instance
 * of this class. This class tracks the state/progress of an action sequence.
 */
@BitStruct(backwardCompatible = true)
class AreaActionsState(

	/**
	 * The current node that is being executed, initially the root node of the action sequence.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "action nodes")
	var node: ActionNode?,

) : BitPostInit {

	/**
	 * `nextPartyPositions(i)` indicates the position to which the party member at index `i` is currently walking, or
	 * `null` if the party member is standing still at `partyPositions(i)`.
	 *
	 * Unlike in `AreaState`, every party member can walk independently of each other, and do *not necessarily*
	 * follow party member 0.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val nextPartyPositions = Array<NextAreaPosition?>(4) { null }

	/**
	 * The chat log, which contains all the previously-encountered dialogue of the current action sequence.
	 */
	@BitField(id = 2)
	val chatLog = ArrayList<ChatLogEntry>()

	/**
	 * Whether the chat log should be shown during talk/dialogue actions.
	 * The player can toggle this by pressing the L key.
	 */
	var showChatLog = false

	/**
	 * When inside a choice dialogue, this is the index of the currently selected option. Otherwise, this should be 0.
	 */
	var selectedChoice = 0
		private set

	/**
	 * When inside a dialogue, this is the number of characters (glyphs) that should be rendered in the box. When a
	 * dialogue node is reached, `shownDialogueCharacters` starts at 0, and slowly increases. Pressing 'E' or 'Q' speeds
	 * it up dramatically.
	 */
	var shownDialogueCharacters = 0f
		private set

	/**
	 * When the current action is `ActionSwitchArea`, the game should transition to that new area... after a small
	 * fade-out effect. If this field is non-negative, the game should transition to that area once
	 * `currentTime >= switchAreaAt`.
	 */
	var switchAreaAt = (-1).seconds
		private set

	private var speedUpShowingCharacters = false
	private var finishedTalkingAction = false

	/**
	 * The color of the most-recently reached `ActionFlashScreen`, or 0 if no such action has been reached (yet).
	 */
	var lastFlashColor = 0
		private set

	/**
	 * The result of `System.nanoTime()` when the most recent `ActionFlashScreen` was reached, or `ZERO` if no such
	 * action has been reached (yet).
	 */
	var lastFlashTime = 0L
		private set

	/**
	 * When the current action is `ActionSaveCampaign`, this field tracks the savefile selection of the user. The user
	 * can either select a `SaveFile` to overwrite, or create a new `SaveFile`.
	 */
	var saveSelectionState: SaveSelectionState? = null
		private set

	internal constructor() : this(FixedActionNode())

	override fun postInit(context: BitPostInit.Context) {
		val node = this.node
		if (node is FixedActionNode && node.action is ActionSaveCampaign) {
			this.node = node.next
		}
	}

	private fun updateFixedNode(context: UpdateContext, currentAction: FixedAction): Boolean {
		if (currentAction is ActionTalk) {
			val finishedMessage = updateTalking(context, currentAction)
			if (finishedMessage) {
				chatLog.add(ChatLogEntry(
					speaker = currentAction.speaker.getDisplayName(context.party) ?: "no-one?",
					speakerElement = currentAction.speaker.getElement(context.party),
					text = currentAction.text,
				))
			}
			return finishedMessage
		}
		if (currentAction is ActionWalk) return updateWalking(currentAction, context)
		if (currentAction is ActionBattle) {
			context.startBattle = currentAction
			return true
		}
		if (currentAction is ActionFlashScreen) {
			lastFlashColor = currentAction.color
			lastFlashTime = System.nanoTime()
			return true
		}
		if (currentAction is ActionPlaySound) {
			context.soundQueue.insert(currentAction.sound)
			return true
		}
		if (currentAction is ActionHealParty) {
			context.healParty()
			return true
		}
		if (currentAction is ActionSaveCampaign && saveSelectionState == null) {
			saveSelectionState = SaveSelectionState(arrayOf(context.campaignName))
		}
		if (currentAction is ActionFadeCharacter) {
			fadeCharacter(currentAction, context)
			return true
		}
		if (currentAction is ActionRotate) {
			rotate(currentAction, context)
			return true
		}
		if (currentAction is ActionSetMoney) {
			context.setMoney = currentAction.amount
			return true
		}
		if (currentAction is ActionTeleport) {
			teleport(currentAction, context)
			return true
		}
		if (currentAction is ActionToArea) {
			if (switchAreaAt < ZERO) switchAreaAt = context.currentTime + AreaState.DOOR_OPEN_DURATION
			if (context.currentTime >= switchAreaAt) context.switchArea = currentAction
			return false
		}
		if (currentAction is ActionTimelineTransition) {
			context.transitionTimeline(currentAction.timeline, currentAction.newNode)
			return true
		}
		if (currentAction is ActionParallel) {
			// Note that we should ALWAYS update all parallel actions, so we can NOT use something like
			// return currentAction.actions.all { updateFixedNode(context, it) }
			// since the .all {} method will return false as soon as the first action returns false
			var allFinished = true
			for (parallelAction in currentAction.actions) {
				if (!updateFixedNode(context, parallelAction)) allFinished = false
			}
			return allFinished
		}

		return false
	}

	private fun teleport(action: ActionTeleport, context: UpdateContext) {
		when (val target = action.target) {
			is ActionTargetPartyMember -> {
				context.partyPositions[target.index] = AreaPosition(action.x, action.y)
				context.partyDirections[target.index] = action.direction
			}
			is ActionTargetAreaCharacter -> {
				context.characterStates[target.character] = AreaCharacterState(
					x = action.x, y = action.y, direction = action.direction, next = null
				)
			}
			is ActionTargetWholeParty -> {
				for (index in context.partyPositions.indices) {
					context.partyPositions[index] = AreaPosition(action.x, action.y)
				}
				for (index in context.partyDirections.indices) {
					context.partyDirections[index] = action.direction
				}
			}
			else -> throw UnsupportedOperationException("Unsupported action target $target")
		}
	}

	/**
	 * This method should be called during `CampaignState.update(...)` whenever
	 * `areaState.actions != null && areaState.activeBattle == null`
	 */
	fun update(context: UpdateContext) {
		val timeStep = context.timeStep
		while (true) {
			val currentNode = node
			if (currentNode is FixedActionNode) {
				val action = currentNode.action
				if (updateFixedNode(context, action)) {
					toNextNode(currentNode.next)
					context.timeStep = ZERO
					if (action is ActionBattle) return
					continue
				}
			}

			break
		}

		context.timeStep = timeStep
	}

	private fun updateTalking(context: UpdateContext, currentAction: ActionTalk): Boolean {
		var speedModifier = 1f
		if (speedUpShowingCharacters) speedModifier = 20f
		if (context.input.isPressed(InputKey.Cancel)) speedModifier = 20f

		val appearanceSpeed = context.timeStep.toDouble(DurationUnit.SECONDS).toFloat() * speedModifier * 50f
		shownDialogueCharacters = min(
			shownDialogueCharacters + appearanceSpeed,
			currentAction.text.length.toFloat(),
		)

		if (finishedTalkingAction) {
			finishedTalkingAction = false
			return true
		}

		return context.input.isPressed(InputKey.Cancel) && shownDialogueCharacters >= currentAction.text.length
	}

	private fun updateWalking(currentAction: ActionWalk, context: UpdateContext): Boolean {
		val target = currentAction.target
		if (target is ActionTargetWholeParty) return updateWalkingWholeParty(context, currentAction)
		if (target is ActionTargetAreaCharacter) return updateWalkingAreaCharacter(currentAction, target.character, context)
		throw UnsupportedOperationException("Unexpected target $target for $currentAction")
	}

	private fun updateWalkingWholeParty(context: UpdateContext, currentAction: ActionWalk): Boolean {
		val next = nextPartyPositions[0]
		if (next != null && context.currentTime >= next.arrivalTime) {
			for (index in (1 until context.partyPositions.size).reversed()) {
				context.partyPositions[index] = context.partyPositions[index - 1]
				context.partyDirections[index] = context.partyDirections[index - 1]
			}
			context.partyDirections[0] = Direction.exactDelta(
				next.position.x - context.partyPositions[0].x,
				next.position.y - context.partyPositions[0].y
			) ?: context.partyDirections[0]
			context.partyPositions[0] = next.position
			nextPartyPositions[0] = null
		}

		if (nextPartyPositions[0] == null) {
			val nextDirection = Direction.bestDelta(
				currentAction.destinationX - context.partyPositions[0].x,
				currentAction.destinationY - context.partyPositions[0].y,
			)
			if (nextDirection == null) return true

			val arrivalTime = context.currentTime + currentAction.speed.duration
			nextPartyPositions[0] = NextAreaPosition(
				AreaPosition(
					context.partyPositions[0].x + nextDirection.deltaX,
					context.partyPositions[0].y + nextDirection.deltaY,
				), context.currentTime, arrivalTime,
				null,
			)
			context.partyDirections[0] = nextDirection

			for (index in 1 until context.partyPositions.size) {
				val targetPosition = context.partyPositions[index - 1]
				val direction = Direction.exactDelta(
					targetPosition.x - context.partyPositions[index].x,
					targetPosition.y - context.partyPositions[index].y,
				)
				if (direction != null) {
					// Expected case: party member walks to current position of the 'next' party member
					nextPartyPositions[index] = NextAreaPosition(
						targetPosition, context.currentTime, arrivalTime, null
					)
					context.partyDirections[index] = direction
				} else {
					// Edge case: the party member is not on a tile adjacent to the next party member
					// 'solve' this discontinuity by 'teleporting' the party member
					context.partyPositions[index] = targetPosition
					nextPartyPositions[index] = null
				}
			}
		}
		return false
	}

	private fun updateWalkingAreaCharacter(
		currentAction: ActionWalk,
		character: AreaCharacter,
		context: UpdateContext,
	): Boolean {
		var characterState = context.characterStates[character] ?: throw IllegalArgumentException(
			"Missing walk area character $character"
		)

		val nextPosition = characterState.next
		if (nextPosition != null && context.currentTime >= nextPosition.arrivalTime) {
			characterState = AreaCharacterState(
				x = nextPosition.position.x,
				y = nextPosition.position.y,
				direction = characterState.direction,
				next = null,
			)
		}

		if (characterState.next == null) {
			val bestDirection = Direction.bestDelta(
				currentAction.destinationX - characterState.x,
				currentAction.destinationY - characterState.y,
			)
			if (bestDirection != null) {
				characterState = AreaCharacterState(
					x = characterState.x,
					y = characterState.y,
					direction = bestDirection,
					next = NextAreaPosition(
						position = AreaPosition(
							x = characterState.x + bestDirection.deltaX,
							y = characterState.y + bestDirection.deltaY,
						),
						startTime = context.currentTime,
						arrivalTime = context.currentTime + currentAction.speed.duration,
						transition = null,
					)
				)
			} else {
				context.characterStates[character] = characterState
				return true
			}
		}

		context.characterStates[character] = characterState
		return false
	}

	private fun fadeCharacter(action: ActionFadeCharacter, context: UpdateContext) {
		val character = action.target.character
		val characterState = context.characterStates.remove(character) ?: throw IllegalArgumentException(
			"Can't fade character $character that is not present"
		)
		context.fadingCharacters.add(FadingCharacter(character, characterState))
	}

	private fun rotate(action: ActionRotate, context: UpdateContext) {
		when (val target = action.target) {
			is ActionTargetPartyMember -> {
				context.partyDirections[target.index] = action.newDirection
			}

			is ActionTargetAreaCharacter -> {
				val oldState = context.characterStates[target.character] ?: throw IllegalArgumentException(
					"Missing area character ${target.character}"
				)
				if (oldState.next != null) throw IllegalArgumentException(
					"Cannot rotate moving area character ${target.character}"
				)

				context.characterStates[target.character] = AreaCharacterState(
					x = oldState.x,
					y = oldState.y,
					direction = action.newDirection,
					next = null
				)
			}

			else -> throw UnsupportedOperationException("Unsupported rotate target $target")
		}
	}

	/**
	 * The `CampaignState` will call this method after saving has succeeded (or was canceled). This will cause this
	 * `AreaActionsState` to move on to the next action node.
	 */
	fun finishSaveNode() {
		val currentNode = node as FixedActionNode
		if (currentNode.action !is ActionSaveCampaign) {
			throw IllegalStateException("Expected ActionSaveCampaign, but action is ${currentNode.action}")
		}
		toNextNode(currentNode.next)
	}

	/**
	 * Aborts the current `SaveSelectionState`, causing it to be refreshed during the next `update()`
	 */
	fun retrySaveNode() {
		saveSelectionState = null
	}

	private fun processFixedActionKeyEvent(action: FixedAction, event: InputKeyEvent) {
		if (action is ActionTalk) {
			processTalkingKeyEvent(action, event)
		}
		if (action is ActionParallel) {
			for (parallelAction in action.actions) processFixedActionKeyEvent(parallelAction, event)
		}
	}

	/**
	 * This method should be called for each `InputKeyEvent` that is fired while
	 * `areaState.actions != null && areaState.activeBattle == null`
	 */
	fun processKeyEvent(event: InputKeyEvent) {
		if (!event.didPress) return

		val currentNode = node
		val key = event.key

		if (key == InputKey.ToggleChatLog) showChatLog = !showChatLog

		if (currentNode is FixedActionNode) processFixedActionKeyEvent(currentNode.action, event)

		if (currentNode is ChoiceActionNode) {
			if (selectedChoice > 0 && key == InputKey.MoveUp) selectedChoice -= 1
			if (selectedChoice < currentNode.options.size - 1 && key == InputKey.MoveDown) selectedChoice += 1
			if (key == InputKey.Interact && !event.didRepeat) {
				toNextNode(currentNode.options[selectedChoice].next)
			}
		}
	}

	private fun toNextNode(next: ActionNode?) {
		this.node = next
		this.selectedChoice = 0
		this.shownDialogueCharacters = 0f
		this.speedUpShowingCharacters = false
		this.finishedTalkingAction = false
	}

	private fun processTalkingKeyEvent(currentAction: ActionTalk, event: InputKeyEvent) {
		if (event.key == InputKey.Interact) {
			if (shownDialogueCharacters >= currentAction.text.length) {
				if (!event.didRepeat) this.finishedTalkingAction = true
			} else speedUpShowingCharacters = true
		}
	}

	/**
	 * This class contains the 'parameters' that should be supplied to `AreaActionsState.update(context)`
	 */
	class UpdateContext(
		val input: InputManager,
		var timeStep: Duration,
		val soundQueue: SoundQueue,
		val campaignName: String,
		val partyPositions: Array<AreaPosition>,
		val partyDirections: Array<Direction>,
		var currentTime: Duration,
		val party: Array<PlayableCharacter?>,
		val characterStates: MutableMap<AreaCharacter, AreaCharacterState>,
		val fadingCharacters: MutableCollection<FadingCharacter>,
		val story: StoryState,
		val healParty: () -> Unit,
		val transitionTimeline: (Timeline, TimelineNode) -> Unit,
	) {
		var startBattle: ActionBattle? = null
		var setMoney: Int? = null
		var switchArea: ActionToArea? = null
	}

	companion object {
		const val FLASH_DURATION = 750_000_000L
	}
}
