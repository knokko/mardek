package mardek.game.battle

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestStatusEffects {

	fun testBattleRendering(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val state = InGameState(campaign, "test")
			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Auto-Regen" }!!)
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "PSN" }!!)

			startSimpleBattle(campaign)
			val battle = ((campaign.state as AreaState).suspension as AreaSuspensionBattle).battle
			// Skip fade-in
			state.update(GameStateUpdateContext(
				content, titleContent, InputManager(), SoundQueue(), 1.seconds
			))
			val monster = battle.livingOpponents()[0]
			monster.statusEffects.add(content.stats.statusEffects.find { it.flashName == "PAR" }!!)

			val effectColors = arrayOf(
				Color(102, 255, 0), // Poison color
				Color(0, 255, 255), // Regen color
				Color(255, 255, 0), // Paralysis color
			)
			testRendering(
				state, 800, 600, "battle-status-effects",
				effectColors, emptyArray()
			)
		}
	}

	fun testRemoveEffectsAfterWinning(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(
				content, titleContent, InputManager(), SoundQueue(), 10.milliseconds
			)
			performTimelineTransition(updateContext, state.campaign, "MainTimeline", "Childhood")
			val mardekState = state.campaign.characterStates[childMardek]!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Auto-Regen" }!!)

			val battleTrack = content.audio.musicTracks.find { it.fileName == "battle" }!!
			val victoryTrack = content.audio.musicTracks.find { it.fileName == "VictoryFanfare" }!!
			startSimpleBattle(state.campaign)
			val battle = ((state.campaign.state as AreaState).suspension as AreaSuspensionBattle).battle

			val monster = battle.livingOpponents()[0]
			monster.currentHealth = 0

			val sleep = content.stats.statusEffects.find { it.flashName == "SLP" }!!
			val confusion = content.stats.statusEffects.find { it.flashName == "CNF" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val regeneration = content.stats.statusEffects.find { it.flashName == "RGN" }!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.statusEffects.add(confusion)
			combatMardek.statusEffects.add(sleep)
			combatMardek.statusEffects.add(poison)

			val brightVictoryColor = arrayOf(Color(154, 141, 112))
			val basicVictoryColor = arrayOf(Color(254, 241, 178))
			val removeEffectColor = arrayOf(Color(155, 30, 20))
			val lootColor = arrayOf(Color(164, 204, 253))
			val allColors = brightVictoryColor + basicVictoryColor + removeEffectColor + lootColor

			repeat(220) {
				state.update(updateContext)
			}
			testRendering(
				state, 800, 600, "victory-effects1",
				emptyArray(), allColors,
			)
			assertEquals(setOf(sleep, confusion, regeneration, poison), combatMardek.statusEffects)
			assertSame(battleTrack, state.determineMusic(content, content.audio).track)

			repeat(10) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)
			testRendering(
				state, 800, 600, "victory-effects2",
				emptyArray(), allColors
			)
			assertEquals(setOf(sleep, confusion, regeneration, poison), combatMardek.statusEffects)

			repeat(50) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)
			testRendering(
				state, 800, 600, "victory-effects3",
				emptyArray(), allColors
			)

			// Sleep and Confusion should be removed because they are temporary, and not active as AUTO effect
			assertEquals(setOf(regeneration, poison), combatMardek.statusEffects)

			repeat(45) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)

			// The remove indicator for the first removed status effect should be shown (either Sleep or Confusion)
			testRendering(
				state, 800, 600, "victory-effects4",
				brightVictoryColor + removeEffectColor, basicVictoryColor + lootColor
			)

			repeat(25) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)
			testRendering(
				state, 800, 600, "victory-effects5",
				basicVictoryColor + removeEffectColor, brightVictoryColor + lootColor
			)

			repeat(100) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)

			// The remove indicator for the 'first' status effect should be gone by now
			testRendering(
				state, 800, 600, "victory-effects6",
				basicVictoryColor, brightVictoryColor + removeEffectColor + lootColor
			)

			// The remove indicator for the 'second' status effect should be visible now
			repeat(90) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)
			testRendering(
				state, 800, 600, "victory-effects7",
				basicVictoryColor + removeEffectColor, brightVictoryColor + lootColor
			)

			repeat(10) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)

			// The loot fade-in should start now
			testRendering(
				state, 800, 600, "victory-effects8",
				emptyArray(), brightVictoryColor + basicVictoryColor + removeEffectColor
			)

			// The loot fade-in should be finished now
			repeat(40) {
				state.update(updateContext)
			}
			assertSame(victoryTrack, state.determineMusic(content, content.audio).track)
			testRendering(
				state, 800, 600, "victory-effects9",
				lootColor, brightVictoryColor + basicVictoryColor
			)
		}
	}

	fun testPoisonAfterWalking(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.state = AreaState(
				dragonLairEntry, campaign.story, campaign.expressionContext(),
				AreaPosition(5, 9),
			)

			val mardekState = campaign.characterStates[heroMardek]!!
			assertEquals(90, mardekState.currentHealth)
			mardekState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "PSN" }!!)

			val input = InputManager()
			input.postEvent(pressKeyEvent(InputKey.MoveUp))

			val context = CampaignState.UpdateContext(
				GameStateUpdateContext(content, titleContent, input, SoundQueue(), 10.milliseconds), ""
			)

			while ((campaign.state as AreaState).getPlayerPosition(0).y != 4) {
				campaign.update(context)
			}

			// Mardek should have taken 2 damage from poison, since 0.02 * 90 = 1.8, which is rounded to 2
			assertEquals(88, mardekState.currentHealth)
		}
	}
}
