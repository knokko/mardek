package mardek.game.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.action.ActionFadeCharacter
import mardek.content.action.ActionParallel
import mardek.content.action.ActionTalk
import mardek.content.action.ActionToArea
import mardek.content.action.ActionWalk
import mardek.content.action.FixedActionNode
import mardek.content.animation.ColorTransform
import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaCharacterState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.BattleStateMachine
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestDragonLair {

	fun testBossBattle(instance: TestingInstance) {
		instance.apply {
			val area = content.areas.areas.find { it.properties.rawName == "DL_area4" }!!
			val state = InGameState(simpleCampaignState(), "test")
			state.campaign.currentArea = AreaState(area, AreaPosition(6, 20))

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
			assertEquals("MightyHeroes", state.campaign.determineMusicTrack(content))
			repeat(5000) {
				state.update(context)
			}
			assertEquals("MightyHeroes", state.campaign.determineMusicTrack(content))

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
			state.campaign.currentArea!!.actions!!.node!!.run {
				val fixed = this as FixedActionNode
				assertInstanceOf<ActionFadeCharacter>(fixed.action)
			}

			// Let's make the battle short
			val battleState = state.campaign.currentArea!!.activeBattle!!
			battleState.livingOpponents()[0].currentHealth = 1

			// Wait until we can select a move
			assertEquals("BossBattle", state.campaign.determineMusicTrack(content))
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
			assertEquals("VictoryFanfare2", state.campaign.determineMusicTrack(content))
			fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
			fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(context)
			assertNull(state.campaign.currentArea!!.battleLoot)
			assertEquals("MightyHeroes", state.campaign.determineMusicTrack(content))

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

			// Hold Q until we go to Heroes' Den
			fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
			fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
			assertArrayEquals(arrayOf(heroMardek, heroDeugan, null, null), state.campaign.party)
			assertEquals(
				ColorTransform(0, -1, 0),
				state.campaign.story.evaluate(state.campaign.currentArea!!.area.properties.ambience)
			)
			assertEquals("MightyHeroes", state.campaign.determineMusicTrack(content))
			repeat(200) {
				val actionNode = state.campaign.currentArea!!.actions!!.node as FixedActionNode
				if (actionNode.action !is ActionToArea) {
					state.update(context)
				}
			}
			fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))

			// Wait until the Dragon's Lair fade-out is almost over
			repeat(45) {
				state.update(context)
			}
			assertEquals("DL_area4", state.campaign.currentArea!!.area.properties.rawName)
			assertArrayEquals(arrayOf(heroMardek, heroDeugan, null, null), state.campaign.party)
			testRendering(
				state, 1000, 700, "dragon-boss4",
				emptyArray(), baseColors + princessPortraitColors + dragonColors,
			)

			// Wait until the Heroes' Den fade-in just started
			repeat(10) {
				state.update(context)
			}
			assertArrayEquals(arrayOf(childMardek, childDeugan, null, null), state.campaign.party)
			assertEquals("crickets", state.campaign.determineMusicTrack(content))
			assertEquals(
				ColorTransform(
					0, rgb(1f, 0.01f * 80, 0.01f * 70), 0
				),
				state.campaign.story.evaluate(state.campaign.currentArea!!.area.properties.ambience)
			)

			val heroesDenColors = arrayOf(
				Color(104, 103, 42), // Grass
				Color(109, 80, 64), // Rock
				Color(96, 159, 170), // Save crystal
				Color(115, 75, 46), // Roof
				Color(102, 38, 0), // Mardek hair
				Color(195, 125, 53), // Deugan hair
			)
			val childDeuganPortraitColors = arrayOf(
				Color(70, 117, 33), // Tunic
				Color(236, 197, 157), // Skin
			)

			testRendering(
				state, 1000, 700, "dragon-boss5",
				emptyArray(),
				baseColors + princessPortraitColors + dragonColors + heroesDenColors,
			)

			// Wait until the fade-out is over
			repeat(50) {
				state.update(context)
			}

			testRendering(
				state, 1000, 700, "dragon-boss6",
				heroesDenColors + childDeuganPortraitColors, dragonColors,
			)

			// Skip the Heroes' Den dialogue
			context.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(500) {
				state.update(context)
			}
			testRendering(
				state, 1000, 700, "dragon-boss7",
				heroesDenColors, baseColors + childDeuganPortraitColors,
			)

			assertEquals(
				AreaPosition(10, 6),
				state.campaign.currentArea!!.getPlayerPosition(0)
			)

			fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
			repeat(25) {
				state.update(context)
			}
			assertEquals(
				AreaPosition(9, 6),
				state.campaign.currentArea!!.getPlayerPosition(0)
			)
		}
	}
}
