package mardek.state.ingame.actions

import mardek.content.Content
import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionFlashScreen
import mardek.content.action.ActionGiveItem
import mardek.content.action.ActionHealParty
import mardek.content.action.ActionParallel
import mardek.content.action.ActionPlaySound
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionTakeItem
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionWalk
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.FixedActionNode
import mardek.content.action.WalkSpeed
import mardek.content.area.Area
import mardek.content.area.Direction
import mardek.content.area.objects.AreaCharacter
import mardek.content.audio.SoundEffect
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.EquipmentSlot
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.content.stats.CombatStat
import mardek.content.stats.StatModifier
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.FadingCharacter
import mardek.state.ingame.area.NextAreaPosition
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
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
	party: Array<PlayableCharacter?> = arrayOf(PlayableCharacter(), null, null, null),
	playableCharacterStates: Map<PlayableCharacter, CharacterState> = emptyMap(),
	characterStates: Map<AreaCharacter, AreaCharacterState> = mutableMapOf(),
	fadingCharacters: List<FadingCharacter> = mutableListOf(),
): AreaActionsState.UpdateContext {

	val content = Content()
	val areaState = AreaState(
		Area(), null, null,
		partyPositions[0], partyDirections[0],
	)
	partyPositions.copyInto(areaState.playerPositions)
	partyDirections.copyInto(areaState.playerDirections)
	areaState.fadingCharacters.addAll(fadingCharacters)
	areaState.characterStates.putAll(characterStates)

	val campaign = CampaignState()
	campaign.state = areaState
	party.copyInto(campaign.party)
	campaign.characterStates.putAll(playableCharacterStates)

	return AreaActionsState.UpdateContext(
		parent = AreaState.UpdateContext(
			parent = CampaignState.UpdateContext(
				parent = GameStateUpdateContext(content, input, soundQueue, timeStep),
				campaignName = campaignName,
			),
			campaign = campaign,
		),
		areaState = areaState,
	)
}

private fun postEvent(actions: AreaActionsState, input: InputManager, event: InputKeyEvent) {
	input.postEvent(event)
	actions.processKeyEvent(createUpdateContext(1.milliseconds), event)
}

private fun pressAndRelease(actions: AreaActionsState, input: InputManager, key: InputKey) {
	postEvent(actions, input, InputKeyEvent(key = key, didPress = true, didRepeat = false, didRelease = false))
	postEvent(actions, input, InputKeyEvent(key = key, didPress = false, didRepeat = false, didRelease = true))
}

class TestAreaActionsState {

	private fun update(actions: AreaActionsState, context: AreaActionsState.UpdateContext) {
		actions.update(context)
		context.areaState.currentTime += context.timeStep
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

		val context = createUpdateContext(
			10.milliseconds,
			partyPositions = arrayOf(
				AreaPosition(2, 3),
				AreaPosition(2, 2),
				AreaPosition(1, 2),
				AreaPosition(1, 1),
			),
			partyDirections = arrayOf(
				Direction.Down,
				Direction.Down,
				Direction.Right,
				Direction.Down,
			),
		)

		val actions = AreaActionsState(rootNode, null)
		postEvent(actions, context.input, InputKeyEvent(
			InputKey.MoveDown, didPress = true, didRepeat = false, didRelease = false
		)) // Pressing this key shouldn't have any effect

		update(actions, context)
		while (context.areaState.playerPositions[0].x != 20) {

			assertEquals(3, context.areaState.playerPositions[0].y)
			assertEquals(Direction.Right, context.areaState.playerDirections[0])

			if (context.areaState.playerPositions[0].x > 2) {
				assertEquals(3, context.areaState.playerPositions[1].y)
				assertEquals(Direction.Right, context.areaState.playerDirections[1])
				assertEquals(context.areaState.playerPositions[0].x, context.areaState.playerPositions[1].x + 1)
			}

			if (context.areaState.playerPositions[0].x > 3) {
				assertEquals(3, context.areaState.playerPositions[2].y)
				assertEquals(Direction.Right, context.areaState.playerDirections[2])
				assertEquals(context.areaState.playerPositions[1].x, context.areaState.playerPositions[2].x + 1)
			}

			if (context.areaState.playerPositions[0].x > 4) {
				assertEquals(3, context.areaState.playerPositions[3].y)
				assertEquals(Direction.Right, context.areaState.playerDirections[3])
				assertEquals(
					context.areaState.playerPositions[2].x,
					context.areaState.playerPositions[3].x + 1,
				)
			}
			assertTrue(context.areaState.currentTime < 4.seconds)

			update(actions, context)
		}

		assertArrayEquals(arrayOf(
			AreaPosition(20, 3),
			AreaPosition(19, 3),
			AreaPosition(18, 3),
			AreaPosition(17, 3),
		), context.areaState.playerPositions)

		update(actions, context)
		while (context.areaState.playerPositions[0] != AreaPosition(10, 13)) {
			assertTrue(
				context.areaState.playerDirections[0] == Direction.Left ||
						context.areaState.playerDirections[0] == Direction.Down
			)

			if (context.areaState.playerPositions[0].x < 18) {
				assertEquals(
					context.areaState.playerPositions[2], AreaPosition(
						context.areaState.playerPositions[0].x + 1,
						context.areaState.playerPositions[0].y - 1,
					)
				)
				assertEquals(
					context.areaState.playerPositions[3], AreaPosition(
						context.areaState.playerPositions[1].x + 1,
						context.areaState.playerPositions[1].y - 1,
					)
				)
				assertTrue(
					context.areaState.playerPositions[0].x == context.areaState.playerPositions[1].x ||
							context.areaState.playerPositions[0].y == context.areaState.playerPositions[1].y
				)
				assertTrue(
					context.areaState.playerPositions[0].x == context.areaState.playerPositions[1].x - 1 ||
							context.areaState.playerPositions[0].y == context.areaState.playerPositions[1].y + 1
				)

				assertTrue(
					context.areaState.playerDirections[1] == Direction.Left ||
							context.areaState.playerDirections[1] == Direction.Down
				)
				assertEquals(
					context.areaState.playerDirections[0],
					context.areaState.playerDirections[2],
				)
				assertEquals(
					context.areaState.playerDirections[1],
					context.areaState.playerDirections[3],
				)
			}

			assertTrue(context.areaState.currentTime < 19.seconds)
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

		val actions = AreaActionsState(rootNode, null)

		val context = createUpdateContext(1.milliseconds, characterStates = mutableMapOf(Pair(paladin, initialState)))
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
			), context.areaState.characterStates[paladin]!!)
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
			), context.areaState.characterStates[paladin]!!)
		}

		repeat(7000) {
			update(actions, context)
			val direction = context.areaState.characterStates[paladin]!!.direction
			assertTrue(direction == Direction.Left || direction == Direction.Up)
		}

		assertEquals(AreaCharacterState(
			x = 10,
			y = 5,
			direction = Direction.Left,
			next = null,
		), context.areaState.characterStates[paladin]!!)
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

		val actions = AreaActionsState(rootNode, null)
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

		val actions = AreaActionsState(rootNode, null)

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

		val actions = AreaActionsState(rootNode, null)
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

		val actions = AreaActionsState(rootNode, null)

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

		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(10.milliseconds)

		assertNotEquals(0, context.campaign.party.size)
		for (partyMember in context.campaign.party.filterNotNull()) {
			partyMember.baseStats.add(StatModifier(CombatStat.Vitality, 20))
			val state = CharacterState()
			state.currentHealth = 1
			state.currentLevel = 50
			context.campaign.characterStates[partyMember] = state
		}
		repeat(5) {
			update(actions, context)
		}

		assertTrue((actions.node as FixedActionNode).action is ActionWalk)
		for (partyMember in context.campaign.usedPartyMembers()) {
			assertNotEquals(1, partyMember.state.currentHealth)
		}
	}

	@Test
	fun testDialogueChoices() {
		val rootNode = ChoiceActionNode(
			id = UUID.randomUUID(),
			speaker = ActionTargetPartyMember(1),
			options = arrayOf(
				ChoiceEntry(expression = "susp", text = "shut up", next = null),
				ChoiceEntry(expression = "susp", text = "let's move on", next = null),
				ChoiceEntry(expression = "susp", text = "respond", next = ChoiceActionNode(
					id = UUID.randomUUID(),
					speaker = ActionTargetPartyMember(0),
					options = arrayOf(
						ChoiceEntry(expression = "norm", text = "hello to you", next = null),
						ChoiceEntry(expression = "norm", text = "let's kill some zombies", next = null),
					)
				)),
			)
		)

		val actions = AreaActionsState(rootNode, null)
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

		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionFadeCharacter(ActionTargetAreaCharacter(dragon)),
			next = null,
		)

		val context = createUpdateContext(
			1.seconds, characterStates = mutableMapOf(Pair(dragon, lastDragonState))
		)

		val actions = AreaActionsState(rootNode, null)

		val beforeUpdate = System.nanoTime()
		update(actions, context)

		assertEquals(0, context.areaState.characterStates.size)
		assertEquals(1, context.areaState.fadingCharacters.size)
		val fading = context.areaState.fadingCharacters.iterator().next()
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
		val actions = AreaActionsState(rootNode, null)

		val context = createUpdateContext(
			10.milliseconds, characterStates = mapOf(Pair(princess, initialPrincessState))
		)

		// The first update should rotate both the player and the princess
		update(actions, context)
		assertArrayEquals(arrayOf(
			Direction.Up, Direction.Right, Direction.Up, Direction.Up
		), context.areaState.playerDirections)
		assertEquals(AreaCharacterState(
			x = 5,
			y = 0,
			direction = Direction.Left,
			next = null
		), context.areaState.characterStates[princess])
		assertEquals(1, context.areaState.characterStates.size)
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
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(10.milliseconds, characterStates = mutableMapOf(
			Pair(princess, initialPrincessState),
			Pair(dragon, initialDragonState),
		))

		// The dragon should need 6 seconds to reach its destination
		repeat(605) {
			update(actions, context)
			assertEquals(Direction.Up, context.areaState.characterStates[princess]!!.direction)
			assertEquals(Direction.Up, context.areaState.characterStates[dragon]!!.direction)
		}

		assertEquals(AreaCharacterState(
			x = 10,
			y = 3,
			direction = Direction.Up,
			next = null
		), context.areaState.characterStates[dragon])

		// At this point, the princess should have moved only 12 tiles
		context.areaState.characterStates[princess]!!.apply {
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
		), context.areaState.characterStates[dragon])
		assertEquals(AreaCharacterState(
			x = 4,
			y = 7,
			direction = Direction.Left,
			next = null
		), context.areaState.characterStates[princess])
		assertNull(actions.node)
	}

	@Test
	fun testSetOverlayColor() {
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionWalk(
				target = ActionTargetWholeParty(),
				destinationX = 5,
				destinationY = 0,
				speed = WalkSpeed.Normal,
			),
			next = FixedActionNode(
				id = UUID.randomUUID(),
				action = ActionSetOverlayColor(
					color = 1234, transitionTime = 300.milliseconds
				),
				next = FixedActionNode(
					id = UUID.randomUUID(),
					action = ActionWalk(
						target = ActionTargetWholeParty(),
						destinationX = 0,
						destinationY = 0,
						speed = WalkSpeed.Normal,
					),
					next = FixedActionNode(
						id = UUID.randomUUID(),
						action = ActionSetOverlayColor(color = 0, transitionTime = 1.seconds),
						next = null,
					),
				),
			),
		)

		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(10.milliseconds)

		repeat(100) {
			update(actions, context)
			assertEquals(Duration.ZERO, actions.startOverlayTransitionTime)
			assertEquals(0, actions.overlayColor)
			assertInstanceOf<ActionWalk>((actions.node as FixedActionNode).action)
		}

		repeat(30) {
			update(actions, context)
			assertEquals(1.seconds, actions.startOverlayTransitionTime)
			assertEquals(0, actions.overlayColor)
			assertInstanceOf<ActionSetOverlayColor>((actions.node as FixedActionNode).action)
		}

		repeat(100) {
			update(actions, context)
			assertEquals(1234, actions.overlayColor)
			assertInstanceOf<ActionWalk>((actions.node as FixedActionNode).action)
		}

		repeat(100) {
			update(actions, context)
			assertEquals(2300.milliseconds, actions.startOverlayTransitionTime)
			assertEquals(1234, actions.overlayColor)
			assertInstanceOf<ActionSetOverlayColor>((actions.node as FixedActionNode).action)
		}

		update(actions, context)
		assertNull(actions.node)
	}

	@Test
	fun testTakeItems() {
		val fairy = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionTakeItem(fairy, 10),
			next = null,
		)

		val member1 = PlayableCharacter()
		val member2 = PlayableCharacter()

		val state1 = CharacterState()
		val state2 = CharacterState()

		val slot1 = EquipmentSlot(UUID.randomUUID(), "", emptyArray(), true)
		val slot2 = EquipmentSlot(UUID.randomUUID(), "", emptyArray(), true)
		val slot3 = EquipmentSlot(UUID.randomUUID(), "", emptyArray(), false)

		state1.inventory[1] = ItemStack(otherItem, 2)
		state1.inventory[3] = ItemStack(fairy, 1)
		state1.inventory[4] = ItemStack(fairy, 2)
		state1.equipment[slot1] = otherItem
		state1.equipment[slot2] = fairy
		state1.equipment[slot3] = fairy

		state2.inventory[4] = ItemStack(fairy, 5)
		state2.equipment[slot1] = fairy
		state2.equipment[slot2] = fairy

		val characterStates = mapOf(Pair(member1, state1), Pair(member2, state2))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member1, null, null, member2),
			playableCharacterStates = characterStates,
		)
		update(actions, context)

		assertEquals(ItemStack(otherItem, 2), state1.inventory[1])
		assertNull(state1.inventory[3])
		assertNull(state1.inventory[4])
		assertSame(otherItem, state1.equipment[slot1])
		assertFalse(state1.equipment.containsKey(slot2))
		assertSame(fairy, state1.equipment[slot3]) // Note that slot3 must NOT be empty

		assertNull(state2.inventory[4])
		assertEquals(1, state2.equipment.size)
		assertSame(fairy, state2.equipment.values.iterator().next())
	}

	@Test
	fun testGiveItemsToFirstInventoryNewStack() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member = PlayableCharacter()
		val state = CharacterState()
		state.inventory[0] = ItemStack(otherItem, 1)

		val characterStates = mapOf(Pair(member, state))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member, null, null, null),
			playableCharacterStates = characterStates,
		)
		update(actions, context)
		assertEquals(ItemStack(otherItem, 1), state.inventory[0])
		assertEquals(ItemStack(reward, 10), state.inventory[1])
		assertNull(state.inventory[2])
		assertTrue(context.campaign.itemStorage.isEmpty())
	}

	@Test
	fun testGiveItemsToFirstInventoryExistingStack() {
		val reward = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member = PlayableCharacter()
		val state = CharacterState()
		state.inventory[0] = ItemStack(reward, 1)

		val characterStates = mapOf(Pair(member, state))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member, null, null, null),
			playableCharacterStates = characterStates,
		)
		update(actions, context)
		assertEquals(ItemStack(reward, 11), state.inventory[0])
		assertNull(state.inventory[1])
		assertTrue(context.campaign.itemStorage.isEmpty())
	}

	@Test
	fun testGiveItemsToSecondInventoryNewStack() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member1 = PlayableCharacter()
		val member2 = PlayableCharacter()

		val state1 = CharacterState()
		val state2 = CharacterState()

		state1.inventory.fill(ItemStack(otherItem, 123))

		val characterStates = mapOf(Pair(member1, state1), Pair(member2, state2))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member1, null, null, member2),
			playableCharacterStates = characterStates,
		)
		update(actions, context)

		assertEquals(ItemStack(reward, 10), state2.inventory[0])
		assertNull(state2.inventory[1])
	}

	@Test
	fun testGiveItemsToSecondInventoryExistingStack() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member1 = PlayableCharacter()
		val member2 = PlayableCharacter()

		val state1 = CharacterState()
		val state2 = CharacterState()

		state1.inventory.fill(ItemStack(otherItem, 123))
		state2.inventory[0] = ItemStack(reward, 5)

		val characterStates = mapOf(Pair(member1, state1), Pair(member2, state2))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member1, null, null, member2),
			playableCharacterStates = characterStates,
		)
		update(actions, context)

		assertEquals(ItemStack(reward, 15), state2.inventory[0])
		assertNull(state2.inventory[1])
	}

	@Test
	fun testGiveItemsToItemStorageNewStack() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member = PlayableCharacter()
		val state = CharacterState()
		state.inventory.fill(ItemStack(otherItem, 1))

		val characterStates = mapOf(Pair(member, state))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member, null, null, null),
			playableCharacterStates = characterStates,
		)
		context.campaign.itemStorage.add(null)
		context.campaign.itemStorage.add(null)
		update(actions, context)
		assertEquals(0, state.countItemOccurrences(reward))
		assertEquals(
			listOf(ItemStack(reward, 10), null),
			context.campaign.itemStorage,
		)
	}

	@Test
	fun testGiveItemsToItemStorageExistingStack() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member = PlayableCharacter()
		val state = CharacterState()
		state.inventory.fill(ItemStack(otherItem, 1))

		val characterStates = mapOf(Pair(member, state))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member, null, null, null),
			playableCharacterStates = characterStates,
		)
		context.campaign.itemStorage.add(ItemStack(reward, 5))
		context.campaign.itemStorage.add(null)
		update(actions, context)
		assertEquals(0, state.countItemOccurrences(reward))
		assertEquals(
			listOf(ItemStack(reward, 15), null),
			context.campaign.itemStorage,
		)
	}

	@Test
	fun testGiveItemsToItemStorageExpand() {
		val reward = Item()
		val otherItem = Item()
		val rootNode = FixedActionNode(
			id = UUID.randomUUID(),
			action = ActionGiveItem(reward, 10),
			next = null,
		)

		val member = PlayableCharacter()
		val state = CharacterState()
		state.inventory.fill(ItemStack(otherItem, 1))

		val characterStates = mapOf(Pair(member, state))
		val actions = AreaActionsState(rootNode, null)
		val context = createUpdateContext(
			10.milliseconds,
			party = arrayOf(member, null, null, null),
			playableCharacterStates = characterStates,
		)
		update(actions, context)
		assertEquals(0, state.countItemOccurrences(reward))
		assertEquals(listOf(ItemStack(reward, 10)), context.campaign.itemStorage)
	}
}
