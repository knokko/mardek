package mardek.game.area

import mardek.content.action.ActionParallel
import mardek.content.action.ActionTalk
import mardek.content.action.ActionWalk
import mardek.content.action.FixedActionNode
import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.BattleStateMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

fun testDragonLairDoor(instance: TestingInstance) {
	instance.apply {
		val state = InGameState(CampaignState(
			currentArea = AreaState(dragonLairEntry, AreaPosition(5, 8)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		), "test")

		val doorColor = Color(60, 38, 27)
		val hairColorDeugan = Color(195, 156, 77)
		val expectedEntryColors = arrayOf(
			Color(59, 53, 68), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(86, 50, 86), // color of braziers
			Color(101, 50, 0), // hair color of Mardek
			Color(70, 117, 33), // cape color of Deugan
			Color(96, 199, 242), // the save crystal
		)

		testRendering(
			state, 1000, 800, "dragon-lair-entry1",
			expectedEntryColors + doorColor, arrayOf(hairColorDeugan)
		)

		val dummySoundQueue = SoundQueue()
		val fakeInput = InputManager()
		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = true, didRelease = false, didRepeat = false))

		val context = GameStateUpdateContext(content, fakeInput, dummySoundQueue, 10.milliseconds)
		repeat(5000) {
			state.update(context)
		}

		testRendering(
			state, 1000, 800, "dragon-lair-entry2",
			expectedEntryColors + hairColorDeugan, emptyArray()
		)

		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = false, didRelease = true, didRepeat = false))
		fakeInput.postEvent(InputKeyEvent(InputKey.Interact, didPress = true, didRelease = false, didRepeat = false))

		repeat(2000) {
			state.update(context)
		}

		val expectedRoomColors = arrayOf(
			Color(59, 53, 68), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(13, 0, 22), // dark blue for the rest of the background
			Color(101, 50, 0), // hair color of Mardek
		)
		testRendering(
			state, 1000, 800, "dragon-lair-room2",
			expectedRoomColors, arrayOf(hairColorDeugan)
		)
	}
}

fun testDragonLairBossBattle(instance: TestingInstance) {
	instance.apply {
		val area = content.areas.areas.find { it.properties.rawName == "DL_area4" }!!
		val state = InGameState(CampaignState(
			currentArea = AreaState(area, AreaPosition(6, 20)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		), "test")

		// Make sure Deugan has enough mana to cast Frostasia
		val deuganState = state.campaign.characterStates[heroDeugan]!!
		deuganState.currentLevel = 50
		state.campaign.healParty()

		// Make sure Deugan has more agility than the dragon
		deuganState.equipment[4] = content.items.items.find { it.flashName == "Boots of Celerity" }!!
		deuganState.equipment[5] = deuganState.equipment[4]

		val dummySoundQueue = SoundQueue()
		val fakeInput = InputManager()
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveUp))

		// Walk to the walk trigger
		val context = GameStateUpdateContext(content, fakeInput, dummySoundQueue, 10.milliseconds)
		repeat(5000) {
			state.update(context)
		}

		val baseColors = arrayOf(
			Color(71, 117, 34), // Deugan cape
			Color(102, 50, 0), // Mardek hair
			Color(255, 134, 231), // Princess
			Color(77, 69, 95), // Floor
		)
		val dragonColors = arrayOf(
			Color(255, 157, 0),
			Color(56, 34, 73),
		)
		val princessPortraitColors = arrayOf(
			Color(255, 181, 255),
			Color(232, 185, 138),
		)
		testRendering(
			state, 1000, 700, "dragon-boss1",
			baseColors + dragonColors, princessPortraitColors,
		)

		// Skip dialogue
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveUp))
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		repeat(5000) {
			state.update(context)
		}

		// Let's make the battle short
		val battleState = state.campaign.currentArea!!.activeBattle!!
		battleState.livingOpponents()[0].currentHealth = 1

		// Wait until we can select a move
		assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
		Thread.sleep(1000)
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		state.update(context)

		// Cast Frostasia, which should kill the 1HP dragon
		assertInstanceOf<BattleStateMachine.SelectMove>(battleState.state)
		for (key in arrayOf(
			InputKey.MoveLeft, InputKey.Interact,
			InputKey.MoveDown, InputKey.MoveDown,
			InputKey.Interact, InputKey.Interact,
		)) {
			fakeInput.postEvent(pressKeyEvent(key))
			state.update(context)
			fakeInput.postEvent(releaseKeyEvent(key))
		}

		assertInstanceOf<BattleStateMachine.CastSkill>(battleState.state)
		val castState = battleState.state as BattleStateMachine.CastSkill
		castState.hasFinishedCastingAnimation = true
		castState.canSpawnTargetParticles = true
		castState.targetParticlesSpawnTime = System.nanoTime() - 5_000_000_000L
		state.update(context)

		assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
		Thread.sleep(1000)
		state.update(context)

		assertInstanceOf<BattleStateMachine.Victory>(battleState.state)
		Thread.sleep(2000)
		state.update(context)

		// Claim battle loot
		assertNotNull(state.campaign.currentArea!!.battleLoot)
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertNull(state.campaign.currentArea!!.battleLoot)

		// Wait 1 second for the dragon to fade away
		val actions = state.campaign.currentArea!!.actions!!
		assertInstanceOf<ActionTalk>((actions.node as FixedActionNode).action)
		Thread.sleep(1000)
		repeat(100) {
			state.update(context)
		}
		testRendering(
			state, 1000, 700, "dragon-boss2",
			baseColors, dragonColors + princessPortraitColors,
		)

		// Move on to the next dialogue node: a parallel node
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)

		// The princess should walk towards Mardek while Mardek is speaking.
		// We try to skip the dialogue by holding Q and pressing E,
		// but it must NOT go to the next node until the princess arrives!
		val parallelActions = (actions.node as FixedActionNode).action as ActionParallel
		assertInstanceOf<ActionWalk>(parallelActions.actions[0])
		assertInstanceOf<ActionTalk>(parallelActions.actions[1])
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		repeat(50) {
			state.update(context)
		}
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		state.update(context)
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		state.update(context)
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSame(parallelActions, (actions.node as FixedActionNode).action)

		// Wait until the princess arrives
		repeat(400) {
			state.update(context)
		}

		val princess = area.objects.characters.find { it.name == "Princess" }!!
		assertEquals(AreaCharacterState(
			x = 6,
			y = 7,
			direction = Direction.Down,
			next = null,
		), state.campaign.currentArea!!.getCharacterState(princess))
		assertSame(parallelActions, (actions.node as FixedActionNode).action)

		// Now, press E again to finish this dialogue node
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		state.update(context)

		assertInstanceOf<ActionTalk>((actions.node as FixedActionNode).action)
		testRendering(
			state, 1000, 700, "dragon-boss3",
			baseColors + princessPortraitColors, dragonColors,
		)
		// TODO CHAP1 Continue the test in Hero's Den
	}
}
