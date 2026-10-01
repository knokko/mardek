package mardek.game.area

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.input.InputKey
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.area.AreaSuspensionIncomingRandomBattle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import kotlin.time.Duration.Companion.milliseconds

object TestRandomBattles {

	fun testNoEncountersBefore30Steps(instance: TestingInstance) {
		instance.apply {
			repeat(1000) {
				val campaign = simpleCampaignState()
				campaign.state = AreaState(
					dragonLair2, campaign.story, campaign.expressionContext(),
					AreaPosition(7, 35),
				)
				campaign.triggers.activateTrigger(dragonLair2.objects.walkTriggers[0])


				val context = CampaignState.UpdateContext(
					createUpdateContext(10.milliseconds), ""
				)
				context.input.postEvent(pressKeyEvent(InputKey.MoveUp))

				while ((campaign.state as AreaState).getPlayerPosition(0).y != 6) {
					assertFalse((campaign.state as AreaState).suspension is AreaSuspensionIncomingRandomBattle)
					assertFalse((campaign.state as AreaState).suspension is AreaSuspensionBattle)
					campaign.update(context)
				}

				assertEquals(29L, campaign.statistics.totalSteps)
				assertEquals(29, campaign.stepsSinceLastBattle)
			}
		}
	}

	fun testEncountersAfter30Steps(instance: TestingInstance) {
		instance.apply {
			var numEncounters = 0
			repeat(10_000) {
				val campaign = simpleCampaignState()
				campaign.state = AreaState(
					dragonLair2, campaign.story, campaign.expressionContext(),
					AreaPosition(7, 39),
				)
				campaign.stepsSinceLastBattle = 30
				campaign.triggers.activateTrigger(dragonLair2.objects.walkTriggers[0])

				val context = CampaignState.UpdateContext(
					createUpdateContext(10.milliseconds), ""
				)
				context.input.postEvent(pressKeyEvent(InputKey.MoveUp))

				while ((campaign.state as AreaState).getPlayerPosition(0).y != 2) {
					campaign.update(context)
					if ((campaign.state as AreaState).suspension is AreaSuspensionIncomingRandomBattle) break
				}

				if ((campaign.state as AreaState).suspension is AreaSuspensionIncomingRandomBattle) numEncounters += 1
			}

			assertTrue(numEncounters in 9740 .. 9940, "Expected $numEncounters to be 9840")
		}
	}

	fun testEncountersAfter60Steps(instance: TestingInstance) {
		instance.apply {
			var numEncounters = 0
			repeat(10_000) {
				val campaign = simpleCampaignState()
				campaign.state = AreaState(
					dragonLair2, campaign.story, campaign.expressionContext(),
					AreaPosition(7, 39),
				)
				campaign.stepsSinceLastBattle = 60
				campaign.triggers.activateTrigger(dragonLair2.objects.walkTriggers[0])

				val context = CampaignState.UpdateContext(
					createUpdateContext(10.milliseconds), ""
				)
				context.input.postEvent(pressKeyEvent(InputKey.MoveUp))

				while ((campaign.state as AreaState).getPlayerPosition(0).y != 2) {
					campaign.update(context)
					if ((campaign.state as AreaState).suspension is AreaSuspensionIncomingRandomBattle) break
				}

				if ((campaign.state as AreaState).suspension is AreaSuspensionIncomingRandomBattle) numEncounters += 1
			}

			assertTrue(numEncounters in 9950 .. 9995, "Expected $numEncounters to be 9980")
		}
	}

	fun testTransferOddsToNextArea(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.state = AreaState(
				dragonLair2, campaign.story, campaign.expressionContext(),
				AreaPosition(7, 39),
				skipFadeIn = true,
			)
			campaign.stepsSinceLastBattle = 20
			campaign.statistics.totalSteps = 100
			campaign.triggers.activateTrigger(dragonLair2.objects.walkTriggers[0])

			val context = CampaignState.UpdateContext(
				createUpdateContext(600.milliseconds), ""
			)

			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			campaign.update(context)

			context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			campaign.update(context)

			assertEquals(20, campaign.stepsSinceLastBattle)
			assertEquals(100, campaign.statistics.totalSteps)
			assertSame(dragonLairEntry, (campaign.state as AreaState).area)

			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			campaign.update(context)

			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			campaign.update(context)

			assertEquals(20, campaign.stepsSinceLastBattle)
			assertEquals(100, campaign.statistics.totalSteps)
			assertSame(dragonLair2, (campaign.state as AreaState).area)
		}
	}

	fun testCannotOpenDoorWhileBattleIsIncoming(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.state = AreaState(
				dragonLair2, campaign.story, campaign.expressionContext(),
				AreaPosition(7, 3),
				skipFadeIn = true,
			)
			campaign.stepsSinceLastBattle = 500

			val context = CampaignState.UpdateContext(
				createUpdateContext(10.milliseconds), ""
			)

			context.input.postEvent(pressKeyEvent(InputKey.MoveUp))

			repeat(25) {
				campaign.update(context)
			}

			assertInstanceOf<AreaSuspensionIncomingRandomBattle>((campaign.state as AreaState).suspension)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))

			repeat(500) {
				campaign.update(context)
			}

			assertSame(dragonLair2, (campaign.state as AreaState).area)
			assertInstanceOf<AreaSuspensionBattle>((campaign.state as AreaState).suspension)
		}
	}
}
