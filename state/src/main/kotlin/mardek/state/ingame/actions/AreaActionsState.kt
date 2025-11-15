package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.action.*
import mardek.content.area.Direction
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.NextAreaPosition
import mardek.state.saves.SaveSelectionState
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
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

	partyPositions: Array<AreaPosition>,
	partyDirections: Array<Direction>,
) {
	/**
	 * The party member positions that this action state is currently overriding. When an action sequence starts, the
	 * current positions of all party members are copied to `partyPositions`. When the action sequence ends, the
	 * `partyPositions` are copied to the player positions of the `AreaState`. (This seems useless, but has some
	 * serialization benefits.)
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val partyPositions: Array<AreaPosition> = partyPositions.clone()

	/**
	 * The party member directions that this action state is currently overriding. It works like `partyPositions`.
	 */
	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val partyDirections: Array<Direction> = partyDirections.clone()

	/**
	 * `nextPartyPositions(i)` indicates the position to which the party member at index `i` is currently walking, or
	 * `null` if the party member is standing still at `partyPositions(i)`.
	 *
	 * Unlike in `AreaState`, every party member can walk independently of each other, and do *not necessarily*
	 * follow party member 0.
	 */
	@BitField(id = 3)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val nextPartyPositions = Array<NextAreaPosition?>(4) { null }

	/**
	 * The in-game time that has elapsed since the action sequence started
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = true)
	var currentTime = ZERO
		private set

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

	private var speedUpShowingCharacters = false

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

	@Suppress("unused")
	private constructor() : this(FixedActionNode(), emptyArray(), emptyArray())

	/**
	 * This method should be called during `CampaignState.update(...)` whenever
	 * `areaState.actions != null && areaState.activeBattle == null`
	 */
	fun update(context: UpdateContext) {
		val currentNode = node
		if (currentNode is FixedActionNode) {
			val currentAction = currentNode.action
			if (currentAction is ActionTalk) updateTalking(context, currentAction)
			if (currentAction is ActionWalk) updateWalking(currentAction)
			if (currentAction is ActionBattle) {
				node = currentNode.next
				TODO("start battle")
			}
			if (currentAction is ActionFlashScreen) {
				lastFlashColor = currentAction.color
				lastFlashTime = System.nanoTime()
				node = currentNode.next
			}
			if (currentAction is ActionPlaySound) {
				context.soundQueue.insert(currentAction.sound)
				node = currentNode.next
			}
			if (currentAction is ActionHealParty) {
				context.healParty()
				node = currentNode.next
			}
			if (currentAction is ActionSaveCampaign && saveSelectionState == null) {
				saveSelectionState = SaveSelectionState(arrayOf(context.campaignName))
			}
		}

		checkNewNode(currentNode)
		currentTime += context.timeStep
	}

	private fun updateTalking(context: UpdateContext, currentAction: ActionTalk) {
		var speedModifier = 1f
		if (speedUpShowingCharacters) speedModifier = 20f
		if (context.input.isPressed(InputKey.Cancel)) speedModifier = 20f

		val appearanceSpeed = context.timeStep.toDouble(DurationUnit.SECONDS).toFloat() * speedModifier * 50f
		shownDialogueCharacters = min(
			shownDialogueCharacters + appearanceSpeed,
			currentAction.text.length.toFloat(),
		)

		val speaker = currentAction.speaker
		if (speaker is ActionTargetPartyMember) {
			partyDirections[0] = Direction.bestDelta(
				partyPositions[1].x - partyPositions[0].x,
				partyPositions[1].y - partyPositions[0].y,
			) ?: Direction.Down
			for (index in 1 until partyDirections.size) {
				partyDirections[index] = Direction.bestDelta(
					partyPositions[index - 1].x - partyPositions[index].x,
					partyPositions[index - 1].y - partyPositions[index].y,
				) ?: Direction.Up
			}
		}

		if (context.input.isPressed(InputKey.Cancel) && shownDialogueCharacters >= currentAction.text.length) {
			finishSimpleDialogueNode()
		}
	}

	private fun updateWalking(currentAction: ActionWalk) {
		val target = currentAction.target
		if (target is ActionTargetWholeParty) updateWalkingWholeParty(currentAction)
	}

	private fun updateWalkingWholeParty(currentAction: ActionWalk) {
		val next = nextPartyPositions[0]
		if (next != null && currentTime >= next.arrivalTime) {
			for (index in (1 until partyPositions.size).reversed()) {
				partyPositions[index] = partyPositions[index - 1]
				partyDirections[index] = partyDirections[index - 1]
			}
			partyDirections[0] = Direction.exactDelta(
				next.position.x - partyPositions[0].x,
				next.position.y - partyPositions[0].y
			) ?: partyDirections[0]
			partyPositions[0] = next.position
			nextPartyPositions[0] = null
		}

		if (nextPartyPositions[0] == null) {
			val nextDirection = Direction.bestDelta(
				currentAction.destinationX - partyPositions[0].x,
				currentAction.destinationY - partyPositions[0].y,
			)
			if (nextDirection == null) {
				node = (node as FixedActionNode).next
				return
			}

			val arrivalTime = currentTime + currentAction.speed.duration
			nextPartyPositions[0] = NextAreaPosition(
				AreaPosition(
					partyPositions[0].x + nextDirection.deltaX,
					partyPositions[0].y + nextDirection.deltaY,
				), currentTime, arrivalTime
			)
			partyDirections[0] = nextDirection

			for (index in 1 until partyPositions.size) {
				val targetPosition = partyPositions[index - 1]
				val direction = Direction.exactDelta(
					targetPosition.x - partyPositions[index].x,
					targetPosition.y - partyPositions[index].y,
				)
				if (direction != null) {
					// Expected case: party member walks to current position of the 'next' party member
					nextPartyPositions[index] = NextAreaPosition(targetPosition, currentTime, arrivalTime)
					partyDirections[index] = direction
				} else {
					// Edge case: the party member is not on a tile adjacent to the next party member
					// 'solve' this discontinuity by 'teleporting' the party member
					partyPositions[index] = targetPosition
					nextPartyPositions[index] = null
				}
			}
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
		node = currentNode.next
	}

	/**
	 * Aborts the current `SaveSelectionState`, causing it to be refreshed during the next `update()`
	 */
	fun retrySaveNode() {
		saveSelectionState = null
	}

	/**
	 * This method should be called for each `InputKeyEvent` that is fired while
	 * `areaState.actions != null && areaState.activeBattle == null`
	 */
	fun processKeyEvent(event: InputKeyEvent) {
		if (!event.didPress) return

		val currentNode = node
		val key = event.key

		if (currentNode is FixedActionNode) {
			val currentAction = currentNode.action
			if (currentAction is ActionTalk) processTalkingKeyEvent(currentAction, event)
		}

		if (currentNode is ChoiceActionNode) {
			if (selectedChoice > 0 && key == InputKey.MoveUp) selectedChoice -= 1
			if (selectedChoice < currentNode.options.size - 1 && key == InputKey.MoveDown) selectedChoice += 1
			if (key == InputKey.Interact && !event.didRepeat) {
				node = currentNode.options[selectedChoice].next
			}
		}

		checkNewNode(currentNode)
	}

	private fun checkNewNode(current: ActionNode?) {
		if (current !== node) {
			selectedChoice = 0
			shownDialogueCharacters = 0f
		}
	}

	private fun processTalkingKeyEvent(currentAction: ActionTalk, event: InputKeyEvent) {
		if (event.key == InputKey.Interact) {
			if (shownDialogueCharacters >= currentAction.text.length) {
				if (!event.didRepeat) finishSimpleDialogueNode()
			} else speedUpShowingCharacters = true
		}
	}

	private fun finishSimpleDialogueNode() {
		node = (node as FixedActionNode).next
		speedUpShowingCharacters = false
	}

	/**
	 * This class contains the 'parameters' that should be supplied to `AreaActionsState.update(context)`
	 */
	class UpdateContext(
		val input: InputManager,
		val timeStep: Duration,
		val soundQueue: SoundQueue,
		val campaignName: String,
		val healParty: () -> Unit,
	)

	companion object {
		const val FLASH_DURATION = 750_000_000L
	}
}
