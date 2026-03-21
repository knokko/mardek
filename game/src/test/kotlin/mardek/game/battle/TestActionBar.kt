package mardek.game.battle

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestActionBar {

	fun testDisabledButtons(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			val deuganState = state.campaign.characterStates[heroDeugan]!!
			deuganState.currentHealth = 10
			startSimpleBattle(state.campaign)

			val battle = ((state.campaign.state as AreaState).suspension as AreaSuspensionBattle).battle
			battle.state = BattleStateMachine.NextTurn(System.nanoTime()) // Skip waiting
			battle.startTime = System.nanoTime() - 1000_000_000L // Skip fade-in
			state.update(updateContext)

			val attackColor = arrayOf(Color(255, 204, 102))
			val disabledAttackColor = arrayOf(Color(94, 77, 55))
			val powersColor = arrayOf(Color(255, 230, 145))
			val itemsColor = arrayOf(Color(150, 255, 207))
			val disabledItemsColor = arrayOf(Color(69, 95, 76))
			testRendering(
				state, 800, 600, "action-bar0",
				attackColor + powersColor + itemsColor,
				disabledItemsColor,
			)

			val combatDeugan = battle.livingPlayers()[1]
			combatDeugan.statusEffects.add(content.stats.statusEffects.find { it.flashName == "NUM" }!!)
			deuganState.inventory.fill(null)

			testRendering(
				state, 800, 600, "action-bar1",
				disabledAttackColor + powersColor + disabledItemsColor,
				attackColor + itemsColor,
			)

			combatDeugan.statusEffects.add(content.stats.statusEffects.find { it.flashName == "SIL" }!!)
			testRendering(
				state, 800, 600, "action-bar2",
				disabledAttackColor + disabledItemsColor,
				attackColor + itemsColor,
			)

			while (updateContext.soundQueue.take() != null) updateContext.soundQueue.take()

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			val sounds = content.audio.fixedEffects.ui
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			assertEquals(
				BattleMoveSelectionAttack(null),
				(battle.state as BattleStateMachine.SelectMove).selectedMove
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertSame(sounds.scroll1, updateContext.soundQueue.take())
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			assertEquals(
				BattleMoveSelectionSkill(null, null),
				(battle.state as BattleStateMachine.SelectMove).selectedMove
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertSame(sounds.scroll1, updateContext.soundQueue.take())
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			assertEquals(
				BattleMoveSelectionItem(null, null),
				(battle.state as BattleStateMachine.SelectMove).selectedMove
			)
		}
	}
}
