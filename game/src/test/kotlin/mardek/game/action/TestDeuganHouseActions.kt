package mardek.game.action

import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import kotlin.time.Duration.Companion.milliseconds

object TestDeuganHouseActions {

	fun testAddPollyToEncyclopedia(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_Dhouse" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(1, 3), Direction.Up,
			)

			val oldEncyclopedia = state.campaign.encyclopedia.createSnapshot(content.encyclopedia, state.campaign)
			assertEquals(0, oldEncyclopedia.people.count { it.entry != null })

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}

			val newEncyclopedia = state.campaign.encyclopedia.createSnapshot(content.encyclopedia, state.campaign)
			assertEquals(1, newEncyclopedia.people.count { it.entry != null })
			assertEquals("Polly", newEncyclopedia.people.find { it.entry != null }!!.entry!!.firstName)
			assertSame(
				content.encyclopedia.people.find { it.snapshots[0].firstName == "Polly" }!!.snapshots[0],
				newEncyclopedia.people.find { it.entry != null }!!.entry,
			)
		}
	}

	fun testAddCrystalsToEncyclopedia(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_Dhouse" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(3, 1), Direction.Up,
			)

			val oldEncyclopedia = state.campaign.encyclopedia.createSnapshot(content.encyclopedia, state.campaign)
			assertEquals(0, oldEncyclopedia.artefacts.count { it.entry != null })

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}

			val newEncyclopedia = state.campaign.encyclopedia.createSnapshot(content.encyclopedia, state.campaign)
			assertEquals(6, newEncyclopedia.artefacts.count { it.entry != null })
			assertSame(
				content.encyclopedia.artefacts.find { it.name == "Fire Crystal" }!!,
				newEncyclopedia.artefacts.find { it.entry?.name == "Fire Crystal" }!!.entry,
			)
		}
	}
}
