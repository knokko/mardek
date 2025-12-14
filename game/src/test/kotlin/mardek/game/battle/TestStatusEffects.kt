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
import org.junit.jupiter.api.Assertions.assertEquals
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

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
			val battle = campaign.currentArea!!.activeBattle!!
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

	fun testPoisonAfterWalking(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.currentArea = AreaState(dragonLairEntry, AreaPosition(5, 9))

			val mardekState = campaign.characterStates[heroMardek]!!
			assertEquals(54, mardekState.currentHealth)
			mardekState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "PSN" }!!)

			val input = InputManager()
			input.postEvent(pressKeyEvent(InputKey.MoveUp))

			val context = CampaignState.UpdateContext(
				GameStateUpdateContext(content, input, SoundQueue(), 10.milliseconds), ""
			)

			while (campaign.currentArea!!.getPlayerPosition(0).y != 4) {
				campaign.update(context)
			}

			// Mardek should have taken 1 damage from poison
			assertEquals(53, mardekState.currentHealth)
		}
	}
}
