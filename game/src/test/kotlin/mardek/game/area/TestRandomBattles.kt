package mardek.game.area

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds

object TestRandomBattles {

	fun testNoEncountersBefore30Steps(instance: TestingInstance) {
		instance.apply {
			repeat(1000) {
				val campaign = CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(7, 35)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
				campaign.triggers.activeTrigger(dragonLair2.objects.walkTriggers[0])

				val input = InputManager()
				input.postEvent(pressKeyEvent(InputKey.MoveUp))

				val context = CampaignState.UpdateContext(
					GameStateUpdateContext(content, input, SoundQueue(), 10.milliseconds), ""
				)

				while (campaign.currentArea!!.getPlayerPosition(0).y != 6) {
					assertNull(campaign.currentArea!!.incomingRandomBattle)
					assertNull(campaign.currentArea!!.activeBattle)
					campaign.update(context)
				}

				assertEquals(campaign.totalSteps, 29L)
				assertEquals(campaign.stepsSinceLastBattle, 29)
			}
		}
	}

	fun testEncountersAfter30Steps(instance: TestingInstance) {
		instance.apply {
			var numEncounters = 0
			repeat(10_000) {
				val campaign = CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(7, 39)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
				campaign.stepsSinceLastBattle = 30
				campaign.triggers.activeTrigger(dragonLair2.objects.walkTriggers[0])

				val input = InputManager()
				input.postEvent(pressKeyEvent(InputKey.MoveUp))

				val context = CampaignState.UpdateContext(
					GameStateUpdateContext(content, input, SoundQueue(), 10.milliseconds), ""
				)

				while (campaign.currentArea!!.getPlayerPosition(0).y != 2) {
					campaign.update(context)
					if (campaign.currentArea!!.incomingRandomBattle != null) break
				}

				if (campaign.currentArea!!.incomingRandomBattle != null) numEncounters += 1
			}

			assertTrue(numEncounters in 9740 .. 9940, "Expected $numEncounters to be 9840")
		}
	}

	fun testEncountersAfter60Steps(instance: TestingInstance) {
		instance.apply {
			var numEncounters = 0
			repeat(10_000) {
				val campaign = CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(7, 39)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
				campaign.stepsSinceLastBattle = 60
				campaign.triggers.activeTrigger(dragonLair2.objects.walkTriggers[0])

				val input = InputManager()
				input.postEvent(pressKeyEvent(InputKey.MoveUp))

				val context = CampaignState.UpdateContext(
					GameStateUpdateContext(content, input, SoundQueue(), 10.milliseconds), ""
				)

				while (campaign.currentArea!!.getPlayerPosition(0).y != 2) {
					campaign.update(context)
					if (campaign.currentArea!!.incomingRandomBattle != null) break
				}

				if (campaign.currentArea!!.incomingRandomBattle != null) numEncounters += 1
			}

			assertTrue(numEncounters in 9950 .. 9995, "Expected $numEncounters to be 9980")
		}
	}

	fun testTransferOddsToNextArea(instance: TestingInstance) {
		instance.apply {
			val campaign = CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(7, 39)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			)
			campaign.stepsSinceLastBattle = 20
			campaign.totalSteps = 100
			campaign.triggers.activeTrigger(dragonLair2.objects.walkTriggers[0])

			val input = InputManager()
			val context = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, SoundQueue(), 600.milliseconds), ""
			)

			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			campaign.update(context)

			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			campaign.update(context)

			assertEquals(20, campaign.stepsSinceLastBattle)
			assertEquals(100, campaign.totalSteps)
			assertSame(dragonLairEntry, campaign.currentArea!!.area)

			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveUp))
			campaign.update(context)

			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			campaign.update(context)

			assertEquals(20, campaign.stepsSinceLastBattle)
			assertEquals(100, campaign.totalSteps)
			assertSame(dragonLair2, campaign.currentArea!!.area)
		}
	}
}
