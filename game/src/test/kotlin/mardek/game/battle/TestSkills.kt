package mardek.game.battle

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestSkills {

	fun testSmiteEvilFlow(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val smiteEvil = heroMardek.characterClass.skillClass.actions.find { it.name == "Smite Evil" }!!
			val monster = content.battle.monsters.find { it.name == "monster" }!!

			// Make sure Mardek gets on turn first, since his sword cannot miss
			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50
			mardekState.currentMana = 20
			mardekState.equipment[4] = content.items.items.find { it.flashName == "RingOfAGL+2" }!!
			mardekState.equipment[5] = mardekState.equipment[4]

			startSimpleBattle(campaign, arrayOf(Enemy(monster, 10), Enemy(monster, 10), null, null))

			val input = InputManager()
			val soundQueue = SoundQueue()
			fun context(timeStep: Duration) = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, timeStep), ""
			)

			campaign.update(context(1.milliseconds))
			sleep(1000)
			campaign.update(context(1.seconds))

			val battle = campaign.currentArea!!.activeBattle!!
			val mardek = battle.livingPlayers()[0]
			val monsterState = battle.livingOpponents()[0]
			battle.state.let {
				assertTrue(it is BattleStateMachine.SelectMove)
				assertSame(mardek, (it as BattleStateMachine.SelectMove).onTurn)
				assertEquals(BattleMoveSelectionAttack(null), it.selectedMove)
			}

			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			campaign.update(context(1.milliseconds))

			assertEquals(1480, monsterState.currentHealth)
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.MoveTo)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.MoveTo).attacker)
				assertSame(monsterState, it.target)
				assertSame(smiteEvil, it.skill)
				assertFalse(it.finished)
			}

			val state = InGameState(campaign, "test")
			val playerColors = arrayOf(
				Color(129, 129, 79), // Mardek pants
				Color(70, 117, 33), // Deugan coat
			)
			val monsterColor = arrayOf(Color(85, 56, 133))

			sleep(1000)
			testRendering(
				state, 800, 450, "smite-evil1",
				playerColors + monsterColor, emptyArray(),
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.MoveTo).finished)
			campaign.update(context(1.seconds))
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.Strike)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.Strike).attacker)
				assertSame(monsterState, it.target)
				assertSame(smiteEvil, it.skill)
				assertFalse(it.finished)
				assertFalse(it.canDealDamage)
			}

			sleep(1000)
			testRendering(
				state, 800, 450, "smite-evil2",
				playerColors + monsterColor, emptyArray()
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.Strike).canDealDamage)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.Strike).finished)
			campaign.update(context(1.seconds))
			assertEquals(0, monsterState.currentHealth)
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.JumpBack)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.JumpBack).attacker)
				assertSame(monsterState, it.target)
				assertSame(smiteEvil, it.skill)
				assertFalse(it.finished)
			}

			sleep(1000)
			testRendering(
				state, 800, 450, "smite-evil3",
				playerColors + monsterColor, emptyArray(),
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.JumpBack).finished)
			campaign.update(context(1.seconds))
			assertInstanceOf<BattleStateMachine.NextTurn>(battle.state)

			sleep(1000)
			campaign.update(context(1.seconds))
			battle.state.let {
				assertSame(battle.livingPlayers()[1], (it as BattleStateMachine.SelectMove).onTurn)
				assertEquals(BattleMoveSelectionAttack(null), it.selectedMove)
			}
			testRendering(
				state, 800, 450, "smite-evil4",
				playerColors + monsterColor, emptyArray()
			)
		}
	}

	fun testRecoverFlow(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val recover = heroDeugan.characterClass.skillClass.actions.find { it.name == "Recover" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!

			// Make sure Mardek gets on turn first, since his sword cannot miss
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.currentHealth = 10
			deuganState.currentMana = 20
			deuganState.activeStatusEffects.add(poison)
			deuganState.skillMastery[recover] = recover.masteryPoints

			startSimpleBattle(campaign)

			val input = InputManager()
			val soundQueue = SoundQueue()
			fun context(timeStep: Duration) = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, timeStep), ""
			)

			campaign.update(context(1.milliseconds))
			sleep(1000)
			campaign.update(context(1.seconds))
			campaign.update(context(1.milliseconds))

			val battle = campaign.currentArea!!.activeBattle!!
			val deugan = battle.livingPlayers()[1]
			battle.state.let {
				assertInstanceOf<BattleStateMachine.SelectMove>(it)
				assertSame(deugan, it.onTurn)
				assertEquals(BattleMoveSelectionAttack(null), it.selectedMove)
			}

			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveUp))
			input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			campaign.update(context(1.milliseconds))

			// Deugan should take 3 damage from poison
			assertEquals(10 - 3, deugan.currentHealth)
			assertEquals(setOf(poison), deugan.statusEffects)
			val castSkill = battle.state as BattleStateMachine.CastSkill
			assertSame(deugan, castSkill.caster)
			assertArrayEquals(arrayOf(deugan), castSkill.targets)
			assertSame(recover, castSkill.skill)
			assertNull(castSkill.reactionChallenge)
			assertNull(castSkill.calculatedDamage)
			assertFalse(castSkill.canSpawnTargetParticles)
			assertEquals(0L, castSkill.targetParticlesSpawnTime)

			val state = InGameState(campaign, "test")
			val playerColors = arrayOf(
				Color(129, 129, 79), // Mardek pants
				Color(70, 117, 33), // Deugan coat
			)
			val monsterColor = arrayOf(Color(85, 56, 133))

			sleep(1000)
			testRendering(
				state, 800, 450, "recover1",
				playerColors + monsterColor, emptyArray(),
			)
			assertTrue(castSkill.canSpawnTargetParticles)

			val beforeUpdateTime = System.nanoTime()
			campaign.update(context(1.seconds))
			assertTrue(castSkill.targetParticlesSpawnTime > beforeUpdateTime)

			sleep(1000)
			testRendering(
				state, 800, 450, "recover2",
				playerColors + monsterColor, emptyArray(),
			)
			campaign.update(context(1.seconds))
			assertArrayEquals(arrayOf(null), castSkill.calculatedDamage)

			assertEquals(deugan.maxHealth, deugan.currentHealth)
			assertEquals(0, deugan.currentMana)
			assertEquals(0, deugan.statusEffects.size)
			assertEquals(deugan.maxHealth, deugan.currentHealth)
			assertInstanceOf<BattleStateMachine.NextTurn>(battle.state)

			sleep(1000)
			campaign.update(context(1.seconds))
			val selection = battle.state as BattleStateMachine.SelectMove
			assertSame(battle.livingPlayers()[0], selection.onTurn)
			assertEquals(BattleMoveSelectionAttack(null), selection.selectedMove)
		}
	}
}
