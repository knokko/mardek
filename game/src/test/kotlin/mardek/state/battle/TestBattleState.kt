package mardek.state.battle

import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.CombatantState
import org.junit.jupiter.api.Assertions.assertEquals

object TestBattleState {

	fun testListPlayersAndEnemies(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))
			val mardekState = state.campaign.characterStates[heroMardek]!!
			val deuganState = state.campaign.characterStates[heroDeugan]!!
			mardekState.currentHealth = 0
			deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats, deuganState.activeStatusEffects)
			startSimpleBattle(state)

			val battle = state.campaign.currentArea!!.activeBattle!!
			assertEquals(listOf(battle.allPlayers()[1]), battle.livingPlayers())
			assertEquals(2, battle.allPlayers().size)

			battle.opponents[1] = null
			assertEquals(emptyList<CombatantState>(), battle.livingOpponents())
		}
	}
}
