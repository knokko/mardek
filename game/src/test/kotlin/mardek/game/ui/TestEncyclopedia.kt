package mardek.game.ui

import mardek.content.area.Direction
import mardek.content.encyclopedia.EncyclopediaPerson
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionEncyclopediaPersonValue
import mardek.content.expression.GreaterEqualStateCondition
import mardek.content.expression.SwitchCaseStateExpression
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.menu.EncyclopediaTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestEncyclopedia {

	fun testPeopleContent(instance: TestingInstance) {
		instance.apply {
			val deugan = content.encyclopedia.people[1]
			assertEquals(3, deugan.snapshots.size)
			assertInstanceOf<SwitchCaseStateExpression<Int, Int>>(deugan.chooseSnapshot)

			val childDeuganSnapshot = deugan.snapshots[0]
			assertEquals("Deugan", childDeuganSnapshot.firstName)
			assertEquals("Selmae Eh-Deredu", childDeuganSnapshot.lastName)
			assertEquals("deugan_child", childDeuganSnapshot.portrait.flashName)
			assertEquals("norm", childDeuganSnapshot.portraitExpression)
			assertSame(childDeugan.creatureType, childDeuganSnapshot.creatureType)
			assertEquals(EncyclopediaPerson.Gender.Male, childDeuganSnapshot.gender)
			assertEquals(10, childDeuganSnapshot.initialAge)
			assertEquals("Goznor, Belfan", childDeuganSnapshot.origin)
			assertSame(
				content.items.items.find { it.displayName == "Balmung" }!!.type,
				childDeuganSnapshot.weaponType
			)
			assertSame(childDeugan.element, childDeuganSnapshot.element)
			assertSame(childDeugan.characterClass, childDeuganSnapshot.characterClass)
			assertEquals("Wannabe Hero", childDeuganSnapshot.characterClass.displayName)
			assertEquals(EncyclopediaPerson.Alignment.NeutralGood, childDeuganSnapshot.alignment)
			assertTrue(childDeuganSnapshot.description.startsWith("Mardek's best friend"))

			assertEquals("Deceased?", deugan.snapshots[2].overrideAge)
			assertNull(deugan.snapshots[2].initialAge)

			val enki = content.encyclopedia.people.find { it.snapshots[0].firstName == "Enki" }!!
			assertEquals(1, enki.snapshots.size)
			assertInstanceOf<ConstantStateExpression<ExpressionEncyclopediaPersonValue>>(enki.chooseSnapshot)
			val enkiSnapshot = enki.snapshots[0]
			assertEquals("susp", enkiSnapshot.portraitExpression)
			assertSame(enkiSnapshot.creatureType, childDeuganSnapshot.creatureType)
			assertEquals("??", enkiSnapshot.overrideAge)
			assertEquals("Katana", enkiSnapshot.weaponType.niceName)
			assertEquals("Wanderer", enkiSnapshot.characterClass.displayName)
		}
	}

	fun testPlacesContent(instance: TestingInstance) {
		instance.apply {
			val goznor = content.encyclopedia.places.find { it.name == "Goznor" }!!
			assertSame(content.battle.backgrounds.find { it.name == "rural" }!!, goznor.background)
			assertTrue(goznor.description.startsWith("A small village"))
			assertInstanceOf<ConstantStateExpression<Boolean>>(goznor.shouldShowUp)

			val castle = content.encyclopedia.places.find { it.name == "Castle Goznor" }!!
			assertSame(content.battle.backgrounds.find { it.name == "castle" }!!, castle.background)
			assertTrue(castle.description.startsWith("A large castle"))
			assertInstanceOf<GreaterEqualStateCondition>(castle.shouldShowUp)
		}
	}

	fun testArtefactsContent(instance: TestingInstance) {
		instance.apply {
			val fireCrystal = content.encyclopedia.artefacts.find { it.name == "Fire Crystal" }!!
			assertSame(content.stats.elements.find { it.rawName == "FIRE" }!!, fireCrystal.element)
			assertTrue(fireCrystal.description.startsWith("One of the"))
			assertInstanceOf<ConstantStateExpression<Boolean>>(fireCrystal.shouldShowUp)

			val etherCrystal = content.encyclopedia.artefacts.find { it.name == "Ether Crystal" }!!
			assertSame(content.stats.elements.find { it.rawName == "ETHER" }!!, etherCrystal.element)
			assertTrue(etherCrystal.description.startsWith("One of the"))
			assertInstanceOf<GreaterEqualStateCondition>(etherCrystal.shouldShowUp)
		}
	}

	fun testBestiary(instance: TestingInstance) {
		instance.apply {
			val dragon = content.encyclopedia.monsters.find { encyclopediaMonster ->
				encyclopediaMonster.monsters[0] === content.battle.monsters.find { it.name == "mightydragon" }
			}!!
			assertEquals(1, dragon.monsters.size)
			assertInstanceOf<ConstantStateExpression<Boolean>>(dragon.shouldShowUp)
			assertTrue(dragon.description.startsWith("The Mighty The Dragon"))

			val bernard = content.encyclopedia.monsters.find { encyclopediaMonster ->
				encyclopediaMonster.monsters.any { it.name == "bernardChapter2" }
			}!!
			assertEquals(2, bernard.monsters.size)
			assertInstanceOf<GreaterEqualStateCondition>(bernard.shouldShowUp)
			assertTrue(bernard.description.startsWith("The black sheep"))
			assertTrue(bernard.monsters.contains(content.battle.monsters.find { it.name == "bernardChapter2" }!!))
			assertTrue(bernard.monsters.contains(content.battle.monsters.find { it.name == "bernardChapter3" }!!))
		}
	}

	fun testBookAddsSocialFoxToEncyclopedia(instance: TestingInstance) {
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
				content.areas.areas.find { it.properties.rawName == "gz_house02" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(4, 1), Direction.Up,
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
			assertEquals("Social Fox", newEncyclopedia.people.find { it.entry != null }!!.entry!!.firstName)
			assertSame(
				content.encyclopedia.people.find { it.snapshots[0].firstName == "Social Fox" }!!.snapshots[0],
				newEncyclopedia.people.find { it.entry != null }!!.entry!!,
			)
		}
	}

	private val forbiddenAreaColors = arrayOf(
		Color(77, 69, 95), // Floor tiles (light)
	)

	private val baseEncyclopediaColors = arrayOf(
		Color(73, 59, 50), // Title bar
		Color(22, 13, 13), // Upper bar
		Color(131, 81, 38), // Upper bar text color
		Color(238, 203, 127), // Section name text color
	)

	fun testPeoplePage(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			state.campaign.encyclopedia.encounteredPeople.add(
				content.encyclopedia.people.find { it.snapshots[0].firstName == "Mardek" }!!
			)
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(4) {
				updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			val outsideColors = arrayOf(
				Color(133, 96, 53), // Selected section icon color
				Color(111, 92, 63), // Unselected section icon color
				Color(51, 153, 204), // Crystal pointer color
				Color(222, 166, 83), // Section name color
				Color(141, 103, 49), // Clock color
			)
			testRendering(
				state, 800, 600, "encyclopedia-outside",
				baseEncyclopediaColors + outsideColors, forbiddenAreaColors,
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val expectedListColors = arrayOf(
				Color(51, 51, 204), // Crystal pointer
				Color(255, 255, 255), // Light element color
				Color(164, 204, 253), // Selected text color
				Color(102, 96, 79), // Unexplored text color
			)
			testRendering(
				state, 800, 600, "encyclopedia-people-list",
				baseEncyclopediaColors + expectedListColors, forbiddenAreaColors,
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val expectedDetailsColors = arrayOf(
				Color(255, 243, 159), // Bold text color
				Color(153, 153, 102), // Mardek portrait tunic color
				Color(236, 197, 157), // Mardek portrait neck color
			)
			testRendering(
				state, 800, 600, "encyclopedia-people-details",
				baseEncyclopediaColors + expectedDetailsColors, forbiddenAreaColors,
			)

			// Go back to the list
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			testRendering(
				state, 800, 600, "encyclopedia-people-list2",
				baseEncyclopediaColors + expectedListColors, forbiddenAreaColors,
			)

			// Go back to the section list
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			testRendering(
				state, 800, 600, "encyclopedia-outside2",
				baseEncyclopediaColors + outsideColors, forbiddenAreaColors,
			)

			// Close the in-game menu
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			testRendering(
				state, 800, 400, "encyclopedia-closed",
				forbiddenAreaColors, emptyArray(),
			)
		}
	}

	fun testPlacesPage(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			state.campaign.encyclopedia.discoveredPlaces.add(
				content.encyclopedia.places.find { it.name == "Goznor" }!!
			)

			// Open encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(4) {
				updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			// Open "Places" list
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Test that pressing Interact does nothing, since we haven't discovered Heroes' Den
			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNull(updateContext.soundQueue.take())

			val expectedListColors = arrayOf(
				Color(51, 51, 204), // Crystal pointer
				Color(164, 204, 253), // Selected text color
				Color(102, 96, 79), // Unexplored text color
			)
			testRendering(
				state, 800, 600, "encyclopedia-places-list",
				baseEncyclopediaColors + expectedListColors, forbiddenAreaColors
			)

			// Open the entry for Goznor
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertSame(content.audio.fixedEffects.ui.scroll1, updateContext.soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			val expectedDetailsColors = arrayOf(
				Color(104, 130, 65), // Grass color on the background hill
				Color(41, 57, 8), // Tree color
				Color(133, 141, 97), // Rock color
			)
			testRendering(
				state, 800, 600, "encyclopedia-places-details",
				baseEncyclopediaColors + expectedDetailsColors, forbiddenAreaColors
			)
		}
	}

	fun testArtefactsPage(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			state.campaign.encyclopedia.discoveredArtefacts.add(
				content.encyclopedia.artefacts.find { it.name == "Water Crystal" }!!
			)
			state.campaign.encyclopedia.discoveredArtefacts.add(
				content.encyclopedia.artefacts.find { it.name == "Fire Crystal" }!!
			)

			// Open encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(4) {
				updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			// Open "Artefacts" list
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			// Open details of the Fire Crystal
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val expectedFireColors = arrayOf(
				Color(204, 64, 15), // Top-right crystal color
				Color(229, 228, 196), // Center crystal color
				Color(201, 137, 26), // Bottom-left crystal color
			)
			testRendering(
				state, 800, 600, "encyclopedia-artefacts-details1",
				baseEncyclopediaColors + expectedFireColors, forbiddenAreaColors
			)

			// Switch to the Water Crystal
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val expectedWaterColors = arrayOf(
				Color(32, 146, 172), // Top-right crystal color
				Color(137, 245, 234), // Center crystal color
				Color(31, 191, 172), // Bottom-left crystal color
			)
			testRendering(
				state, 800, 600, "encyclopedia-artefacts-details2",
				baseEncyclopediaColors + expectedWaterColors, forbiddenAreaColors
			)
		}
	}

	fun testBestiaryPage(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			state.campaign.encyclopedia.reportMonsterAsSlain(
				content.battle.monsters.find { it.name == "mightydragon" }!!
			)

			// Open encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(4) {
				updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			// Open "Bestiary" list
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			// Try to open the details of "Monster", which should not be possible
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNull(updateContext.soundQueue.take())

			val expectedListColors = arrayOf(
				Color(51, 51, 204), // Crystal pointer
				Color(164, 204, 253), // Selected text color
				Color(102, 96, 79), // Unexplored text color
			)
			testRendering(
				state, 800, 600, "encyclopedia-bestiary-list",
				baseEncyclopediaColors + expectedListColors, forbiddenAreaColors
			)

			// Open dragon details
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val expectedDetailsColors = arrayOf(
				Color(255, 243, 159), // Bold text color
				Color(65, 49, 83), // Dragon skin color
				Color(133, 96, 53), // Creature type icon
				Color(61, 61, 75), // Elemental resistance background color
				Color(79, 38, 22), // Elemental weakness background color
				Color(152, 255, 102), // Elemental absorption text color
			)
			testRendering(
				state, 800, 600, "encyclopedia-bestiary-details",
				baseEncyclopediaColors + expectedDetailsColors, forbiddenAreaColors
			)
		}
	}

	fun testRefreshAfterDiscoveringAnotherArea(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(22, 1), Direction.Left,
			)

			// Open encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(4) {
				updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)

			// Only Goznor should be discovered now
			assertEquals(
				1,
				(state.menu.currentTab as EncyclopediaTab).encyclopedia.places.count { it.entry != null }
			)
			assertFalse((state.menu.currentTab as EncyclopediaTab).encyclopedia.places.any {
				it.entry?.name == "Goznor Sewers"
			})
			assertTrue(state.menu.shown)

			// Close the encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			assertFalse(state.menu.shown)

			// Enter the sewers
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			repeat(20) {
				state.update(updateContext)
			}

			// Re-open the encyclopedia
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state.update(updateContext)
			assertTrue(state.menu.shown)

			// Goznor Sewers should be discovered now
			assertEquals(
				2,
				(state.menu.currentTab as EncyclopediaTab).encyclopedia.places.count { it.entry != null }
			)
			assertTrue((state.menu.currentTab as EncyclopediaTab).encyclopedia.places.any {
				it.entry?.name == "Goznor Sewers"
			})
		}
	}
}
