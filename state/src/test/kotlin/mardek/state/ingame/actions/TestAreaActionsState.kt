package mardek.state.ingame.actions

import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionFlashScreen
import mardek.content.action.ActionHealParty
import mardek.content.action.ActionParallel
import mardek.content.action.ActionPlaySound
import mardek.content.action.ActionRotate
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionWalk
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.FixedActionNode
import mardek.content.action.WalkSpeed
import mardek.content.area.Direction
import mardek.content.area.objects.AreaCharacter
import mardek.content.audio.FixedSoundEffects
import mardek.content.audio.SoundEffect
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.FadingCharacter
import mardek.state.ingame.area.NextAreaPosition
import mardek.state.ingame.story.StoryState
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.fail
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private fun createUpdateContext(
	timeStep: Duration,
	input: InputManager = InputManager(),
	soundQueue: SoundQueue = SoundQueue(),
	campaignName: String = "",
	partyPositions: Array<AreaPosition> = Array(4) { AreaPosition() },
	partyDirections: Array<Direction> = Array(4) { Direction.Up },
	currentTime: Duration = Duration.ZERO,
	characterStates: MutableMap<AreaCharacter, AreaCharacterState> = mutableMapOf(),
	fadingCharacters: MutableList<FadingCharacter> = mutableListOf(),
	story: StoryState = StoryState(),
	healParty: () -> Unit = { fail("Attempted to heal party?" )},
) = AreaActionsState.UpdateContext(
	input = input,
	timeStep = timeStep,
	soundQueue = soundQueue,
	sounds = FixedSoundEffects(),
	campaignName = campaignName,
	partyPositions = partyPositions,
	partyDirections = partyDirections,
	currentTime = currentTime,
	party = arrayOf(PlayableCharacter(), null, null, null),
	characterStates = characterStates,
	playableCharacterStates = emptyMap(),
	fadingCharacters = fadingCharacters,
	story = story,
	itemStorage = ArrayList(0),
	healParty = healParty,
	transitionTimeline = { _, _ -> fail("Attempted to transition timeline?" ) },
	getCursorStack = { fail("Attempted to get cursor stack?") },
	setCursorStack = { _ -> fail("Attempted to get cursor stack?") },
)

private fun postEvent(actions: AreaActionsState, input: InputManager, event: InputKeyEvent) {
	input.postEvent(event)
	actions.processKeyEvent(createUpdateContext(1.milliseconds), event)
}

private fun pressAndRelease(actions: AreaActionsState, input: InputManager, key: InputKey) {
	postEvent(actions, input, InputKeyEvent(key = key, didPress = true, didRepeat = false, didRelease = false))
	postEvent(actions, input, InputKeyEvent(key = key, didPress = false, didRepeat = false, didRelease = true))
}

class TestAreaActionsState {

	private var currentTime = Duration.ZERO

	private fun update(actions: AreaActionsState, context: AreaActionsState.UpdateContext) {
		context.currentTime = currentTime
		actions.update(context)
		currentTime += context.timeStep
	}

	@Test
	fun testWalkWithWholeParty() {
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionWalk(
				ActionTargetWholeParty(),
				20,
				3,
				WalkSpeed.Normal,
			),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionWalk(
					ActionTargetWholeParty(),
					10,
					13,
					WalkSpeed.Slow,
				),
				next = null
			)
		)
		val partyPositions = arrayOf(
			AreaPosition(2, 3),
			AreaPosition(2, 2),
			AreaPosition(1, 2),
			AreaPosition(1, 1),
		)
		val partyDirections = arrayOf(
			Direction.Down,
			Direction.Down,
			Direction.Right,
			Direction.Down,
		)

		val context = createUpdateContext(
			10.milliseconds, partyPositions = partyPositions, partyDirections = partyDirections
		)
		val actions = AreaActionsState(rootNode)
		postEvent(actions, context.input, InputKeyEvent(
			InputKey.MoveDown, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing this key shouldn't have any effect

		update(actions, context)
		while (partyPositions[0].x != 20) {

			assertEquals(3, partyPositions[0].y)
			assertEquals(Direction.Right, partyDirections[0])

			if (partyPositions[0].x > 2) {
				assertEquals(3, partyPositions[1].y)
				assertEquals(Direction.Right, partyDirections[1])
				assertEquals(partyPositions[0].x, partyPositions[1].x + 1)
			}

			if (partyPositions[0].x > 3) {
				assertEquals(3, partyPositions[2].y)
				assertEquals(Direction.Right, partyDirections[2])
				assertEquals(partyPositions[1].x, partyPositions[2].x + 1)
			}

			if (partyPositions[0].x > 4) {
				assertEquals(3, partyPositions[3].y)
				assertEquals(Direction.Right, partyDirections[3])
				assertEquals(partyPositions[2].x, partyPositions[3].x + 1)
			}
			assertTrue(currentTime < 4.seconds)

			update(actions, context)
		}

		assertArrayEquals(arrayOf(
			AreaPosition(20, 3),
			AreaPosition(19, 3),
			AreaPosition(18, 3),
			AreaPosition(17, 3),
		), partyPositions)

		update(actions, context)
		while (partyPositions[0] != AreaPosition(10, 13)) {
			assertTrue(partyDirections[0] == Direction.Left || partyDirections[0] == Direction.Down)

			if (partyPositions[0].x < 18) {
				assertEquals(partyPositions[2], AreaPosition(partyPositions[0].x + 1, partyPositions[0].y - 1))
				assertEquals(partyPositions[3], AreaPosition(partyPositions[1].x + 1, partyPositions[1].y - 1))
				assertTrue(partyPositions[0].x == partyPositions[1].x || partyPositions[0].y == partyPositions[1].y)
				assertTrue(partyPositions[0].x == partyPositions[1].x - 1 || partyPositions[0].y == partyPositions[1].y + 1)

				assertTrue(partyDirections[1] == Direction.Left || partyDirections[1] == Direction.Down)
				assertEquals(partyDirections[0], partyDirections[2])
				assertEquals(partyDirections[1], partyDirections[3])
			}

			assertTrue(currentTime < 19.seconds)
			update(actions, context)
		}

		assertNull(actions.node)
	}

	@Test
	fun testWalkAreaCharacter() {
		val paladin = AreaCharacter()
		val initialState = AreaCharacterState(
			x = 20,
			y = 10,
			direction = Direction.Down,
			next = null,
		)
		val characterStates = mutableMapOf(Pair(paladin, initialState))
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionWalk(
				target = ActionTargetAreaCharacter(paladin),
				destinationX = 10,
				destinationY = 5,
				speed = WalkSpeed.Slow,
			),
			next = null,
		)

		val actions = AreaActionsState(rootNode)

		val context = createUpdateContext(1.milliseconds, characterStates = characterStates)
		repeat(495) {
			update(actions, context)
			assertEquals(AreaCharacterState(
				x = 20,
				y = 10,
				direction = Direction.Left,
				next = NextAreaPosition(
					position = AreaPosition(19, 10),
					startTime = Duration.ZERO,
					arrivalTime = 500.milliseconds,
					transition = null,
				),
			), characterStates[paladin]!!)
		}

		repeat(10) {
			update(actions, context)
		}

		repeat(495) {
			update(actions, context)
			assertEquals(AreaCharacterState(
				x = 19,
				y = 10,
				direction = Direction.Left,
				next = NextAreaPosition(
					position = AreaPosition(18, 10),
					startTime = 500.milliseconds,
					arrivalTime = 1000.milliseconds,
					transition = null,
				),
			), characterStates[paladin]!!)
		}

		repeat(7000) {
			update(actions, context)
			val direction = characterStates[paladin]!!.direction
			assertTrue(direction == Direction.Left || direction == Direction.Up)
		}

		assertEquals(AreaCharacterState(
			x = 10,
			y = 5,
			direction = Direction.Left,
			next = null,
		), characterStates[paladin]!!)
		assertNull(actions.node)
	}

	@Test
	fun testNormalTalking() {
		val text1 = Array(100) { "Smile" }.joinToString { it }
		val text2 = Array(100) { "Shut up" }.joinToString { it }
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = text1,
			),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionTalk(
					speaker = ActionTargetPartyMember(1),
					expression = "norm",
					text = text2,
				),
				next = null
			)
		)

		val context = createUpdateContext(10.milliseconds)
		context.input.postEvent(InputKeyEvent(
			InputKey.Interact, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing this key BEFORE THE DIALOGUE shouldn't have any effect

		val actions = AreaActionsState(rootNode)
		update(actions, context)

		repeat(200) {
			update(actions, context)
		}
		assertTrue(2 * actions.shownDialogueCharacters < text1.length)

		postEvent(actions, context.input, InputKeyEvent(
			InputKey.Interact, didPress = false, didRepeat = false, didRelease = true
		)) // Releasing the E key should do nothing... yet

		repeat(200) {
			update(actions, context)
		}
		assertTrue(2 * actions.shownDialogueCharacters < text1.length)

		pressAndRelease(actions, context.input, InputKey.Interact)

		repeat(200) {
			update(actions, context)
		}
		assertEquals(actions.shownDialogueCharacters, text1.length.toFloat())

		// Pressing E again should move the action state to the next dialogue message
		pressAndRelease(actions, context.input, InputKey.Interact)
		update(actions, context)
		assertTrue(
			actions.shownDialogueCharacters < 5f,
			"Expected ${actions.shownDialogueCharacters} < 5"
		)

		// Wait until all characters are visible
		repeat(2000) {
			update(actions, context)
		}
		assertEquals(text2.length.toFloat(), actions.shownDialogueCharacters)

		// End the dialogue
		pressAndRelease(actions, context.input, InputKey.Interact)
		update(actions, context)
		assertNull(actions.node)
	}

	@Test
	fun testSkipTalking() {
		val text1 = Array(100) { "Smile" }.joinToString { it }
		val text2 = Array(100) { "Shut up" }.joinToString { it }
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = text1,
			),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionTalk(
					speaker = ActionTargetPartyMember(1),
					expression = "norm",
					text = text2,
				),
				next = null
			)
		)

		val context = createUpdateContext(10.milliseconds)
		context.input.postEvent(InputKeyEvent(
			InputKey.Cancel, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing Q (cancel) should cause the dialogue to be skipped

		val actions = AreaActionsState(rootNode)

		// Holding Q for 3 seconds should be more than enough
		repeat(300) {
			update(actions, context)
		}
		assertNull(actions.node)
	}

	@Test
	fun testFlashScreen() {
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionWalk(
				target = ActionTargetWholeParty(),
				destinationX = 1,
				destinationY = 0,
				speed = WalkSpeed.Normal,
			),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionFlashScreen(color = 1234),
				next = FixedActionNode(
					id = UUID.randomUUID(),
					action = ActionWalk(
						target = ActionTargetWholeParty(),
						destinationX = 0,
						destinationY = 0,
						speed = WalkSpeed.Normal,
					),
					next = null
				),
			),
		)

		val actions = AreaActionsState(rootNode)
		val context = createUpdateContext(10.milliseconds)

		assertEquals(0L, actions.lastFlashTime)
		assertEquals(0, actions.lastFlashColor)

		val beforeWalking = System.nanoTime()
		while (actions.node === rootNode) {
			update(actions, context)
		}
		val rightAfterFlash = System.nanoTime()

		val flashTime = actions.lastFlashTime
		assertTrue(flashTime in beforeWalking .. rightAfterFlash)
		assertEquals(1234, actions.lastFlashColor)

		assertTrue((actions.node as FixedActionNode).action is ActionWalk)

		while (actions.node != null) {
			update(actions, context)
			assertEquals(flashTime, actions.lastFlashTime)
			assertEquals(1234, actions.lastFlashColor)
		}
	}

	@Test
	fun testPlaySound() {
		val sound = SoundEffect()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionPlaySound(sound),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionWalk(
					target = ActionTargetWholeParty(),
					destinationX = 0,
					destinationY = 1,
					speed = WalkSpeed.Normal,
				),
				next = null
			),
		)

		val actions = AreaActionsState(rootNode)

		val context = createUpdateContext(10.milliseconds)

		assertNull(context.soundQueue.take())
		repeat(5) {
			update(actions, context)
		}

		assertSame(sound, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		assertTrue((actions.node as FixedActionNode).action is ActionWalk)
	}

	@Test
	fun testHealParty() {
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionHealParty(),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionWalk(
					target = ActionTargetWholeParty(),
					destinationX = 0,
					destinationY = 1,
					speed = WalkSpeed.Normal,
				),
				next = null
			),
		)

		val actions = AreaActionsState(rootNode)

		var numHeals = 0
		val context = createUpdateContext(10.milliseconds) { numHeals += 1 }

		repeat(5) {
			update(actions, context)
		}

		assertEquals(1, numHeals)
		assertTrue((actions.node as FixedActionNode).action is ActionWalk)
	}

	@Test
	fun testDialogueChoices() {
		val rootNode = ChoiceActionNode(
			id = UUID.randomUUID(),
			speaker = ActionTargetPartyMember(1),
			expression = "susp",
			options = arrayOf(
				ChoiceEntry(text = "shut up", next = null),
				ChoiceEntry(text = "let's move on", next = null),
				ChoiceEntry(text = "respond", next = ChoiceActionNode(
					id = UUID.randomUUID(),
					speaker = ActionTargetPartyMember(0),
					expression = "norm",
					options = arrayOf(
						ChoiceEntry(text = "hello to you", next = null),
						ChoiceEntry(text = "let's kill some zombies", next = null),
					)
				)),
			)
		)

		val actions = AreaActionsState(rootNode)
		val context = createUpdateContext(10.milliseconds)

		context.input.postEvent(InputKeyEvent(
			InputKey.Cancel, didPress = true, didRepeat = false, didRelease = false
		)) // Cancel shouldn't do anything in choice dialogue nodes

		repeat(10) {
			update(actions, context)
			assertEquals(0, actions.selectedChoice)
		}

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		repeat(10) {
			update(actions, context)
			assertEquals(1, actions.selectedChoice)
		}

		pressAndRelease(actions, context.input, InputKey.MoveUp)
		update(actions, context)
		assertEquals(0, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		update(actions, context)
		assertEquals(1, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		update(actions, context)
		assertEquals(2, actions.selectedChoice)

		assertSame(rootNode, actions.node)
		pressAndRelease(actions, context.input, InputKey.Interact)
		update(actions, context)
		assertEquals(0, actions.selectedChoice)
		assertNotSame(rootNode, actions.node)

		pressAndRelease(actions, context.input, InputKey.MoveUp)
		update(actions, context)
		assertEquals(0, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		update(actions, context)
		assertEquals(1, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.Interact)
		update(actions, context)
		assertNull(actions.node)
	}

	@Test
	fun testFade() {
		val dragon = AreaCharacter()
		val lastDragonState = AreaCharacterState(
			x = 1,
			y = 2,
			direction = Direction.Left,
			next = null,
		)
		val characterStates = mutableMapOf(Pair(dragon, lastDragonState))

		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionFadeCharacter(ActionTargetAreaCharacter(dragon)),
			next = null,
		)

		val context = createUpdateContext(1.seconds, characterStates = characterStates)

		val actions = AreaActionsState(rootNode)

		val beforeUpdate = System.nanoTime()
		update(actions, context)

		assertEquals(0, characterStates.size)
		assertEquals(1, context.fadingCharacters.size)
		val fading = context.fadingCharacters.iterator().next()
		assertSame(dragon, fading.character)
		assertSame(lastDragonState, fading.lastState)
		assertTrue(fading.startFadeTime >= beforeUpdate)
	}

	@Test
	fun testRotate() {
		val princess = AreaCharacter()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionRotate(ActionTargetPartyMember(1), Direction.Right),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionRotate(
					ActionTargetAreaCharacter(princess), Direction.Left
				),
				next = null,
			)
		)

		val initialPrincessState = AreaCharacterState(
			x = 5,
			y = 0,
			direction = Direction.Down,
			next = null,
		)
		val characterStates = mutableMapOf(Pair(princess, initialPrincessState))
		val actions = AreaActionsState(rootNode)

		val context = createUpdateContext(10.milliseconds, characterStates = characterStates)

		// The first update should rotate both the player and the princess
		update(actions, context)
		assertArrayEquals(arrayOf(
			Direction.Up, Direction.Right, Direction.Up, Direction.Up
		), context.partyDirections)
		assertEquals(AreaCharacterState(
			x = 5,
			y = 0,
			direction = Direction.Left,
			next = null
		), characterStates[princess])
		assertEquals(1, characterStates.size)
		assertNull(actions.node)
	}

	@Test
	fun testParallelWalking() {
		val princess = AreaCharacter()
		val dragon = AreaCharacter()

		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionParallel(arrayOf(
				ActionWalk(
					target = ActionTargetAreaCharacter(princess),
					destinationX = 4,
					destinationY = 7,
					speed = WalkSpeed.Slow,
				),
				ActionWalk(
					target = ActionTargetAreaCharacter(dragon),
					destinationX = 10,
					destinationY = 3,
					speed = WalkSpeed.Normal,
				)
			)),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionRotate(
					target = ActionTargetAreaCharacter(dragon),
					newDirection = Direction.Up,
				),
				next = null,
			)
		)

		val initialPrincessState = AreaCharacterState(
			x = 5,
			y = 30,
			direction = Direction.Down,
			next = null,
		)
		val initialDragonState = AreaCharacterState(
			x = 10,
			y = 33,
			direction = Direction.Down,
			next = null,
		)
		val characterStates = mutableMapOf(
			Pair(princess, initialPrincessState),
			Pair(dragon, initialDragonState),
		)
		val actions = AreaActionsState(rootNode)
		val context = createUpdateContext(10.milliseconds, characterStates = characterStates)

		// The dragon should need 6 seconds to reach its destination
		repeat(605) {
			update(actions, context)
			assertEquals(Direction.Up, characterStates[princess]!!.direction)
			assertEquals(Direction.Up, characterStates[dragon]!!.direction)
		}

		assertEquals(AreaCharacterState(
			x = 10,
			y = 3,
			direction = Direction.Up,
			next = null
		), characterStates[dragon])

		// At this point, the princess should have moved only 12 tiles
		characterStates[princess]!!.apply {
			assertEquals(5, x)
			assertEquals(18, y)
			assertEquals(Direction.Up, direction)

			val next = next!!
			assertEquals(5, next.position.x)
			assertEquals(17, next.position.y)
		}

		// The princess needs another 6 seconds
		repeat(600) {
			update(actions, context)
		}

		assertEquals(AreaCharacterState(
			x = 10,
			y = 3,
			direction = Direction.Up,
			next = null
		), characterStates[dragon])
		assertEquals(AreaCharacterState(
			x = 4,
			y = 7,
			direction = Direction.Left,
			next = null
		), characterStates[princess])
		assertNull(actions.node)
	}
}
