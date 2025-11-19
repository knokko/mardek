package mardek.state.ingame.actions

import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionFlashScreen
import mardek.content.action.ActionHealParty
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
import mardek.content.audio.SoundEffect
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.SoundQueue
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.NextAreaPosition
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private fun postEvent(actions: AreaActionsState, input: InputManager, event: InputKeyEvent) {
	input.postEvent(event)
	actions.processKeyEvent(event)
}

private fun pressAndRelease(actions: AreaActionsState, input: InputManager, key: InputKey) {
	postEvent(actions, input, InputKeyEvent(key = key, didPress = true, didRepeat = false, didRelease = false))
	postEvent(actions, input, InputKeyEvent(key = key, didPress = false, didRepeat = false, didRelease = true))
}

class TestAreaActionsState {

	@Test
	fun testWalkWithWholeParty() {
		val rootNode = FixedActionNode(
			action = ActionWalk(
				ActionTargetWholeParty(),
				20,
				3,
				WalkSpeed.Normal,
			),
			next = FixedActionNode(
				action = ActionWalk(
					ActionTargetWholeParty(),
					10,
					13,
					WalkSpeed.Slow,
				),
				next = null
			)
		)
		val oldPartyPositions = arrayOf(
			AreaPosition(2, 3),
			AreaPosition(2, 2),
			AreaPosition(1, 2),
			AreaPosition(1, 1),
		)
		val oldPartyDirections = arrayOf(
			Direction.Down,
			Direction.Down,
			Direction.Right,
			Direction.Down,
		)

		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }

		val actions = AreaActionsState(rootNode, oldPartyPositions, oldPartyDirections)
		postEvent(actions, context.input, InputKeyEvent(
			InputKey.MoveDown, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing this key shouldn't have any effect

		while (actions.partyPositions[0].x != 20) {
			actions.update(context)

			val p = actions.partyPositions
			val d = actions.partyDirections
			assertEquals(3, p[0].y)
			assertEquals(Direction.Right, d[0])

			if (p[0].x > 2) {
				assertEquals(3, p[1].y)
				assertEquals(Direction.Right, d[1])
				assertEquals(p[0].x, p[1].x + 1)
			}

			if (p[0].x > 3) {
				assertEquals(3, p[2].y)
				assertEquals(Direction.Right, d[2])
				assertEquals(p[1].x, p[2].x + 1)
			}

			if (p[0].x > 4) {
				assertEquals(3, p[3].y)
				assertEquals(Direction.Right, d[3])
				assertEquals(p[2].x, p[3].x + 1)
			}
			assertTrue(actions.currentTime < 4.seconds)
		}

		assertArrayEquals(arrayOf(
			AreaPosition(20, 3),
			AreaPosition(19, 3),
			AreaPosition(18, 3),
			AreaPosition(17, 3),
		), actions.partyPositions)

		assertArrayEquals(Array(4) { Direction.Right }, actions.partyDirections)

		actions.update(context)
		while (actions.partyPositions[0] != AreaPosition(10, 13)) {
			val p = actions.partyPositions
			val d = actions.partyDirections

			assertTrue(d[0] == Direction.Left || d[0] == Direction.Down)

			if (p[0].x < 18) {
				assertEquals(p[2], AreaPosition(p[0].x + 1, p[0].y - 1))
				assertEquals(p[3], AreaPosition(p[1].x + 1, p[1].y - 1))
				assertTrue(p[0].x == p[1].x || p[0].y == p[1].y)
				assertTrue(p[0].x == p[1].x - 1 || p[0].y == p[1].y + 1)

				assertTrue(d[1] == Direction.Left || d[1] == Direction.Down)
				assertEquals(d[0], d[2])
				assertEquals(d[1], d[3])
			}

			assertTrue(actions.currentTime < 19.seconds)
			actions.update(context)
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
			action = ActionWalk(
				target = ActionTargetAreaCharacter(paladin),
				destinationX = 10,
				destinationY = 5,
				speed = WalkSpeed.Slow,
			),
			next = null,
		)

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)

		val context = AreaActionsState.UpdateContext(
			InputManager(), 1.milliseconds, SoundQueue(),
			"", characterStates, ArrayList(),
		) { throw UnsupportedOperationException("Attempted to heal party?") }

		repeat(495) {
			actions.update(context)
			assertEquals(AreaCharacterState(
				x = 20,
				y = 10,
				direction = Direction.Left,
				next = NextAreaPosition(
					position = AreaPosition(19, 10),
					startTime = Duration.ZERO,
					arrivalTime = 500.milliseconds,
				),
			), characterStates[paladin]!!)
		}

		repeat(10) {
			actions.update(context)
		}

		repeat(495) {
			actions.update(context)
			assertEquals(AreaCharacterState(
				x = 19,
				y = 10,
				direction = Direction.Left,
				next = NextAreaPosition(
					position = AreaPosition(18, 10),
					startTime = 500.milliseconds,
					arrivalTime = 1000.milliseconds,
				),
			), characterStates[paladin]!!)
		}

		repeat(7000) {
			actions.update(context)
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
			action = ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = text1,
			),
			next = FixedActionNode(
				action = ActionTalk(
					speaker = ActionTargetPartyMember(1),
					expression = "norm",
					text = text2,
				),
				next = null
			)
		)

		val oldPartyPositions = arrayOf(
			AreaPosition(10, 5),
			AreaPosition(11, 5),
			AreaPosition(12, 5),
			AreaPosition(12, 6),
		)
		val oldPartyDirections = arrayOf(
			Direction.Down,
			Direction.Down,
			Direction.Up,
			Direction.Down,
		)

		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }
		context.input.postEvent(InputKeyEvent(
			InputKey.Interact, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing this key BEFORE THE DIALOGUE shouldn't have any effect

		val actions = AreaActionsState(rootNode, oldPartyPositions, oldPartyDirections)


		assertNotSame(oldPartyDirections, actions.partyDirections)
		assertArrayEquals(oldPartyDirections, actions.partyDirections)
		actions.update(context)
		assertNotSame(oldPartyPositions, actions.partyPositions)
		assertArrayEquals(oldPartyPositions, actions.partyPositions)

		repeat(200) {
			actions.update(context)
		}
		assertTrue(2 * actions.shownDialogueCharacters < text1.length)

		postEvent(actions, context.input, InputKeyEvent(
			InputKey.Interact, didPress = false, didRepeat = false, didRelease = true
		)) // Releasing the E key should do nothing... yet

		repeat(200) {
			actions.update(context)
		}
		assertTrue(2 * actions.shownDialogueCharacters < text1.length)

		pressAndRelease(actions, context.input, InputKey.Interact)

		repeat(200) {
			actions.update(context)
		}
		assertEquals(actions.shownDialogueCharacters, text1.length.toFloat())

		// Pressing E again should move the action state to the next dialogue message
		pressAndRelease(actions, context.input, InputKey.Interact)
		actions.update(context)
		assertTrue(
			actions.shownDialogueCharacters < 5f,
			"Expected ${actions.shownDialogueCharacters} < 5"
		)

		// Wait until all characters are visible
		repeat(2000) {
			actions.update(context)
		}
		assertEquals(text2.length.toFloat(), actions.shownDialogueCharacters)

		// End the dialogue
		pressAndRelease(actions, context.input, InputKey.Interact)
		actions.update(context)
		assertNull(actions.node)
	}

	@Test
	fun testSkipTalking() {
		val text1 = Array(100) { "Smile" }.joinToString { it }
		val text2 = Array(100) { "Shut up" }.joinToString { it }
		val rootNode = FixedActionNode(
			action = ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = text1,
			),
			next = FixedActionNode(
				action = ActionTalk(
					speaker = ActionTargetPartyMember(1),
					expression = "norm",
					text = text2,
				),
				next = null
			)
		)

		val oldPartyPositions = Array(4) { AreaPosition()  }
		val oldPartyDirections = Array(4) { Direction.Left }

		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }

		context.input.postEvent(InputKeyEvent(
			InputKey.Cancel, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing Q (cancel) should cause the dialogue to be skipped

		val actions = AreaActionsState(rootNode, oldPartyPositions, oldPartyDirections)

		// Holding Q for 3 seconds should be more than enough
		repeat(300) {
			actions.update(context)
		}
		assertNull(actions.node)
	}

	@Test
	fun testFlashScreen() {
		val rootNode = FixedActionNode(
			action = ActionWalk(
				target = ActionTargetWholeParty(),
				destinationX = 1,
				destinationY = 0,
				speed = WalkSpeed.Normal,
			),
			next = FixedActionNode(
				action = ActionFlashScreen(color = 1234),
				next = FixedActionNode(
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

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)
		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }

		assertEquals(0L, actions.lastFlashTime)
		assertEquals(0, actions.lastFlashColor)

		while ((actions.node as FixedActionNode).action is ActionWalk) {
			actions.update(context)
		}

		val beforeFlash = System.nanoTime()
		assertTrue((actions.node as FixedActionNode).action is ActionFlashScreen)
		actions.update(context)
		val afterFlash = System.nanoTime()

		val flashTime = actions.lastFlashTime
		assertTrue(flashTime in beforeFlash .. afterFlash)
		assertEquals(1234, actions.lastFlashColor)

		assertTrue((actions.node as FixedActionNode).action is ActionWalk)

		while (actions.node != null) {
			actions.update(context)
			assertEquals(flashTime, actions.lastFlashTime)
			assertEquals(1234, actions.lastFlashColor)
		}
	}

	@Test
	fun testPlaySound() {
		val sound = SoundEffect()
		val rootNode = FixedActionNode(
			action = ActionPlaySound(sound),
			next = FixedActionNode(
				action = ActionWalk(
					target = ActionTargetWholeParty(),
					destinationX = 0,
					destinationY = 1,
					speed = WalkSpeed.Normal,
				),
				next = null
			),
		)

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)

		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }

		assertNull(context.soundQueue.take())
		repeat(5) {
			actions.update(context)
		}

		assertSame(sound, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		assertTrue((actions.node as FixedActionNode).action is ActionWalk)
	}

	@Test
	fun testHealParty() {
		val rootNode = FixedActionNode(
			action = ActionHealParty(),
			next = FixedActionNode(
				action = ActionWalk(
					target = ActionTargetWholeParty(),
					destinationX = 0,
					destinationY = 1,
					speed = WalkSpeed.Normal,
				),
				next = null
			),
		)

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)

		var numHeals = 0
		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { numHeals += 1 }

		repeat(5) {
			actions.update(context)
		}

		assertEquals(1, numHeals)
		assertTrue((actions.node as FixedActionNode).action is ActionWalk)
	}

	@Test
	fun testDialogueChoices() {
		val rootNode = ChoiceActionNode(
			speaker = ActionTargetPartyMember(1),
			expression = "susp",
			options = arrayOf(
				ChoiceEntry(text = "shut up", next = null),
				ChoiceEntry(text = "let's move on", next = null),
				ChoiceEntry(text = "respond", next = ChoiceActionNode(
					speaker = ActionTargetPartyMember(0),
					expression = "norm",
					options = arrayOf(
						ChoiceEntry(text = "hello to you", next = null),
						ChoiceEntry(text = "let's kill some zombies", next = null),
					)
				)),
			)
		)

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)
		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", HashMap(), ArrayList(),
		) { fail("Attempted to heal party?") }

		context.input.postEvent(InputKeyEvent(
			InputKey.Cancel, didPress = true, didRepeat = false, didRelease = false
		)) // Cancel shouldn't do anything in choice dialogue nodes

		repeat(10) {
			actions.update(context)
			assertEquals(0, actions.selectedChoice)
		}

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		repeat(10) {
			actions.update(context)
			assertEquals(1, actions.selectedChoice)
		}

		pressAndRelease(actions, context.input, InputKey.MoveUp)
		actions.update(context)
		assertEquals(0, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		actions.update(context)
		assertEquals(1, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		actions.update(context)
		assertEquals(2, actions.selectedChoice)

		assertSame(rootNode, actions.node)
		pressAndRelease(actions, context.input, InputKey.Interact)
		actions.update(context)
		assertEquals(0, actions.selectedChoice)
		assertNotSame(rootNode, actions.node)

		pressAndRelease(actions, context.input, InputKey.MoveUp)
		actions.update(context)
		assertEquals(0, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.MoveDown)
		actions.update(context)
		assertEquals(1, actions.selectedChoice)

		pressAndRelease(actions, context.input, InputKey.Interact)
		actions.update(context)
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
			action = ActionFadeCharacter(ActionTargetAreaCharacter(dragon)),
			next = null,
		)

		val context = AreaActionsState.UpdateContext(
			InputManager(), 1.seconds, SoundQueue(),
			"", characterStates, ArrayList(),
		) { fail("Attempted to heal party") }

		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Right },
		)

		val beforeUpdate = System.nanoTime()
		actions.update(context)

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
			action = ActionRotate(ActionTargetPartyMember(1), Direction.Right),
			next = FixedActionNode(
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
		val actions = AreaActionsState(
			rootNode,
			Array(4) { AreaPosition() },
			Array(4) { Direction.Up },
		)
		val context = AreaActionsState.UpdateContext(
			InputManager(), 10.milliseconds, SoundQueue(),
			"", characterStates, ArrayList(),
		) { fail("Attempted to heal party?") }

		// The first update should rotate the player
		actions.update(context)
		assertArrayEquals(arrayOf(
			Direction.Up, Direction.Right, Direction.Up, Direction.Up
		), actions.partyDirections)
		assertSame(initialPrincessState, characterStates[princess])

		// The second update should rotate the princess
		actions.update(context)
		assertArrayEquals(arrayOf(
			Direction.Up, Direction.Right, Direction.Up, Direction.Up
		), actions.partyDirections)
		assertEquals(AreaCharacterState(
			x = 5,
			y = 0,
			direction = Direction.Left,
			next = null
		), characterStates[princess])
		assertEquals(1, characterStates.size)
		assertNull(actions.node)
	}

	// TODO DL Test parallel walking
}
