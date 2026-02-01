package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.FixedActionNode
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
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionOpeningDoor
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestLocks {

	fun testFakeDragonLairEntryLock(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			state.campaign.state = AreaState(
				dragonLairEntry,
				AreaPosition(5, 10),
				Direction.Down,
			)
			state.campaign.characterStates[heroMardek]!!.currentLevel = 50
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 50

			// Try to open the fake entrance door
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val suspension = (state.campaign.state as AreaState).suspension
			val actions = (suspension as AreaSuspensionActions).actions
			val node = actions.node as FixedActionNode
			assertNull(node.next)
			val action = node.action as ActionTalk
			assertEquals("Entrance", action.speaker.getDisplayName(
				actions.defaultDialogueObject, state.campaign.party
			))
			assertEquals("", action.expression)
			assertEquals("Wait... this isn't a door. It's actually a wall, " +
					"painted to look like a door! How misleading!", action.text)

			// Test that this doesn't crash
			dummySaveManager().createSave(content, state.campaign, "dummy", SaveFile.Type.Cheat)

			// Skip the locked door 'dialogue'
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			assertNull((state.campaign.state as AreaState).suspension)

			// Try to walk away from the entrance
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(100) {
				state.update(updateContext)
			}
			assertEquals(
				AreaPosition(5, 2),
				(state.campaign.state as AreaState).getPlayerPosition(0),
			)
		}
	}

	fun testMonasteryIsNotLockedAtNight(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				AreaPosition(18, 14),
				Direction.Up,
			)

			// Open the monastery door
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val suspension = (state.campaign.state as AreaState).suspension
			assertInstanceOf<AreaSuspensionOpeningDoor>(suspension)

			repeat(20) {
				state.update(updateContext)
			}
			assertSame(
				content.areas.areas.find { it.properties.rawName == "gz_monastery" }!!,
				(state.campaign.state as AreaState).area,
			)
		}
	}

	fun testShopsAreLockedAtNight(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				AreaPosition(31, 11),
				Direction.Up,
			)
			state.campaign.state = areaState

			// Try to enter the weapon shop
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			val node = actions.node as FixedActionNode
			assertNull(node.next)
			val action = node.action as ActionTalk
			assertEquals("Weapon Shop", action.speaker.getDisplayName(
				actions.defaultDialogueObject, state.campaign.party
			))
			assertEquals("", action.expression)
			assertEquals("It's locked.", action.text)

			// Skip the locked door 'dialogue'
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			assertNull((state.campaign.state as AreaState).suspension)

			// Walk to the armour shop
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			while (areaState.suspension == null ||
				(areaState.suspension as AreaSuspensionPlayerWalking).destination.position.x != 37
			) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			while (areaState.suspension != null) {
				state.update(updateContext)
			}

			// Try to enter the armour shop
			assertEquals(AreaPosition(37, 11), areaState.getPlayerPosition(0))
			assertNull(areaState.suspension)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertInstanceOf<AreaSuspensionActions>(areaState.suspension)
		}
	}

	fun testSewerDoorIsLockedDuringChapter1(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "sewer1" }!!,
				AreaPosition(14, 2),
				Direction.Up,
			)
			state.campaign.state = areaState

			// Try to enter the big locked door
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			val node = actions.node as FixedActionNode
			assertNull(node.next)
			val action = node.action as ActionTalk
			assertEquals(
				"Big Door", action.speaker.getDisplayName(
					actions.defaultDialogueObject, state.campaign.party
				)
			)
			assertEquals("", action.expression)
			assertEquals("It's locked.", action.text)
		}
	}

	fun testMardekHouseLockToDropDeugan(instance: TestingInstance) {
		instance.apply {
			val goznorColors = arrayOf(
				Color(77, 90, 95), // Water
				Color(104, 103, 42), // Grass
			)
			val mardekHairColor = arrayOf(Color(102, 38, 0))
			val deuganHairColor = arrayOf(Color(195, 125, 53))
			val deuganPortraitColor = arrayOf(Color(70, 117, 33))
			val allColors = goznorColors + mardekHairColor + deuganHairColor + deuganPortraitColor

			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				AreaPosition(5, 22),
				Direction.Up,
			)
			state.campaign.state = areaState

			// Walk towards the door
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(5) {
				state.update(updateContext)
			}

			// Open the door to the mouse of Mardek, which should start the conversation where Deugan goes home
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNotNull(areaState.suspension)

			// Wait until all dialogue text is shown
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(100) {
				state.update(updateContext)
			}

			testRendering(
				state, 600, 400, "drop-deugan0",
				allColors, arrayOf(),
			)

			// Finish the dialogue
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(5) {
				state.update(updateContext)
			}
			assertArrayEquals(arrayOf(childMardek, childDeugan, null, null), state.campaign.party)
			testRendering(
				state, 600, 400, "drop-deugan1",
				arrayOf(), allColors,
			)

			// After ~500ms, the fade should be complete, and Deugan should be gone
			state.update(updateContext)
			assertArrayEquals(arrayOf(childMardek, null, null, null), state.campaign.party)
			testRendering(
				state, 600, 400, "drop-deugan2",
				arrayOf(), allColors,
			)

			// Wait until the fade is over
			repeat(10) {
				state.update(updateContext)
			}
			assertNull(areaState.suspension)
			testRendering(
				state, 600, 400, "drop-deugan3",
				goznorColors + mardekHairColor, deuganHairColor + deuganPortraitColor,
			)

			// Now that Deugan is dropped, the player should be able to enter the house
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertInstanceOf<AreaSuspensionOpeningDoor>(areaState.suspension)

			repeat(10) {
				state.update(updateContext)
			}
			assertEquals("gz_Mhouse1", (state.campaign.state as AreaState).area.properties.rawName)
		}
	}
}
