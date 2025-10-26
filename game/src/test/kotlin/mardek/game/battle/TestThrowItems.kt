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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestThrowItems {

	fun testElixirFlow(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			// Make sure Mardek gets on turn first, since his sword cannot miss
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.currentHealth = 10
			deuganState.currentMana = 20
			assertEquals(1, deuganState.countItemOccurrences(elixir))

			startSimpleBattle(campaign)

			val input = InputManager()
			val soundQueue = SoundQueue()
			fun context(timeStep: Duration) = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, soundQueue, timeStep), ""
			)

			campaign.update(context(1.milliseconds))
			sleep(1000)
			campaign.update(context(1.seconds))

			val battle = campaign.currentArea!!.activeBattle!!
			val deugan = battle.livingPlayers()[1]

			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
			input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			campaign.update(context(1.milliseconds))

			assertEquals(10, deugan.currentHealth)
			battle.state.let {
				assertTrue(it is BattleStateMachine.UseItem)
				assertSame(deugan, (it as BattleStateMachine.UseItem).thrower)
				assertEquals(deugan, it.target)
				assertSame(elixir, it.item)
				assertFalse(it.canDrinkItem)
			}

			val state = InGameState(campaign, "test")
			val playerColors = arrayOf(
				Color(129, 129, 79), // Mardek pants
				Color(70, 117, 33), // Deugan coat
			)
			val turnOrderColor = arrayOf(Color(132, 96, 53))
			val elixirColor = arrayOf(Color(247, 236, 0))
			val monsterColor = arrayOf(Color(85, 56, 133))

			sleep(1000)
			testRendering(
				state, 800, 450, "elixir1",
				playerColors + monsterColor + elixirColor, turnOrderColor
			)
			assertTrue((battle.state as BattleStateMachine.UseItem).canDrinkItem)
			campaign.update(context(1.seconds))

			assertEquals(deugan.maxHealth, deugan.currentHealth)
			assertEquals(deugan.maxMana, deugan.currentMana)
			assertEquals(0, deuganState.countItemOccurrences(elixir))
			assertInstanceOf<BattleStateMachine.NextTurn>(battle.state)

			sleep(1000)
			campaign.update(context(1.seconds))
			val selection = battle.state as BattleStateMachine.SelectMove
			assertSame(battle.livingPlayers()[0], selection.onTurn)
			assertEquals(BattleMoveSelectionAttack(null), selection.selectedMove)
		}
	}
}
