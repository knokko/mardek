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
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleStateMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestBasicAttacks {

	fun testSimpleFlow(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			// Make sure Mardek gets on turn first, since his sword cannot miss
			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50
			mardekState.equipment[4] = content.items.items.find { it.flashName == "RingOfAGL+2" }!!
			mardekState.equipment[5] = mardekState.equipment[4]

			startSimpleBattle(campaign)

			val input = InputManager()
			val soundQueue = SoundQueue()
			fun context(timeStep: Duration) = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, timeStep), ""
			)

			campaign.update(context(1.milliseconds))
			sleep(1000)
			campaign.update(context(1.seconds))

			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val mardek = battle.livingPlayers()[0]
			val monster = battle.livingOpponents()[0]
			battle.state.let {
				assertTrue(it is BattleStateMachine.SelectMove)
				assertSame(mardek, (it as BattleStateMachine.SelectMove).onTurn)
				assertEquals(BattleMoveSelectionAttack(null), it.selectedMove)
			}

			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			campaign.update(context(1.milliseconds))

			assertEquals(1480, monster.currentHealth)
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.MoveTo)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.MoveTo).attacker)
				assertSame(monster, it.target)
				assertNull(it.skill)
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
				state, 800, 450, "basic-attack1",
				playerColors + monsterColor, emptyArray()
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.MoveTo).finished)
			campaign.update(context(1.seconds))
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.Strike)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.Strike).attacker)
				assertSame(monster, it.target)
				assertNull(it.skill)
				assertFalse(it.finished)
				assertFalse(it.canDealDamage)
			}

			sleep(1000)
			testRendering(
				state, 800, 450, "basic-attack2",
				playerColors + monsterColor, emptyArray()
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.Strike).canDealDamage)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.Strike).finished)
			campaign.update(context(1.seconds))
			assertEquals(0, monster.currentHealth)
			battle.state.let {
				assertTrue(it is BattleStateMachine.MeleeAttack.JumpBack)
				assertSame(mardek, (it as BattleStateMachine.MeleeAttack.JumpBack).attacker)
				assertSame(monster, it.target)
				assertNull(it.skill)
				assertFalse(it.finished)
			}

			sleep(1000)
			testRendering(
				state, 800, 450, "basic-attack3",
				playerColors, monsterColor
			)
			assertTrue((battle.state as BattleStateMachine.MeleeAttack.JumpBack).finished)
			campaign.update(context(1.seconds))
			assertInstanceOf<BattleStateMachine.NextTurn>(battle.state)

			sleep(1000)
			campaign.update(context(1.seconds))
			assertInstanceOf<BattleStateMachine.Victory>(battle.state)
		}
	}
}
