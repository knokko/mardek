package mardek.game.action

import mardek.content.area.Direction
import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import mardek.state.ingame.area.NextAreaPosition
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.worldmap.WorldMapState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestSoothwoodActions {

	fun testPoshGoblin(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)

			var areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(11, 5), Direction.Up,
			)
			state.campaign.state = areaState

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertInstanceOf<AreaSuspensionActions>(areaState.suspension)

			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(10) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))

			val suspension = areaState.suspension as AreaSuspensionBattle

			val battleState = suspension.battle
			assertEquals(48, battleState.livingOpponents()[0].maxHealth)
			assertEquals(48, battleState.livingOpponents()[0].currentHealth)
			assertEquals(60, battleState.livingOpponents()[0].maxMana)
			assertEquals(60, battleState.livingOpponents()[0].currentMana)
			assertEquals(5, battleState.livingOpponents()[0].getStat(
				CombatStat.Attack, battleUpdateContext(state.campaign)
			))

			// Let's finish it quickly
			battleState.livingOpponents()[0].currentHealth = 0

			sleep(800)
			state.update(updateContext)
			assertInstanceOf<BattleStateMachine.Victory>(battleState.state)
			sleep(3100)
			state.update(updateContext)
			assertNotNull(suspension.loot)

			// Claim loot
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Wait until the loot screen fades
			sleep(600)
			state.update(updateContext)
			assertNull(areaState.suspension)

			// Check that PoshGoblin is gone
			val poshGoblin = areaState.area.objects.characters.find { it.name == "Poshgoblin" }!!
			assertNull(areaState.getCharacterState(poshGoblin))
			assertEquals(1, areaState.fadingCharacters.size)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			repeat(10) {
				state.update(updateContext)
			}
			assertEquals(AreaPosition(11, 4), areaState.getPlayerPosition(0))

			// 'Teleport' to the right of the exit
			areaState.suspension = AreaSuspensionPlayerWalking(NextAreaPosition(
				AreaPosition(6, 4), areaState.currentTime,
				areaState.currentTime, null,
			))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))

			repeat(10) {
				state.update(updateContext)
			}

			// Go to the Crash Site
			assertInstanceOf<WorldMapState>(state.campaign.state)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))

			repeat(50) {
				state.update(updateContext)
			}

			// Enter the Crash Site
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(10) {
				state.update(updateContext)
			}

			val crashSiteState = state.campaign.state as AreaState
			assertEquals("Crash Site", crashSiteState.area.properties.displayName)

			// Teleport back to Soothwood
			areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(6, 4), Direction.Up,
			)
			state.campaign.state = areaState

			// Check that Posh Goblin is still gone
			assertNull(areaState.getCharacterState(poshGoblin))

			// Check that the timeline cannot go back if we go through the exit again
			val timeOfDay = content.story.customVariables.find { it.name == "TimeOfDay" }!!
			assertNull(state.campaign.story.evaluate(timeOfDay))

			performTimelineTransition(
				updateContext, state.campaign, "MainTimeline",
				"After the conversation in Rohophs saucer is finished",
			)
			assertEquals("Evening", state.campaign.story.evaluate(timeOfDay))

			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			repeat(10) {
				state.update(updateContext)
			}

			assertInstanceOf<WorldMapState>(state.campaign.state)
			assertEquals("Evening", state.campaign.story.evaluate(timeOfDay))
		}
	}

	fun testNoPoshGoblinAtNight(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "After the conversation in Rohophs saucer is finished"
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "soothwood" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(5, 22), Direction.Up,
			)
			val poshGoblin = areaState.area.objects.characters.find { it.name == "Poshgoblin" }!!
			assertNull(areaState.getCharacterState(poshGoblin))
		}
	}
}
