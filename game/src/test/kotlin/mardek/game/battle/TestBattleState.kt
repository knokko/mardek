package mardek.game.battle

import mardek.game.TestingInstance
import mardek.state.ingame.battle.CombatantState
import org.junit.jupiter.api.Assertions.assertEquals

object TestBattleState {

	fun testListPlayersAndEnemies(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val mardekState = campaign.characterStates[heroMardek]!!
			val deuganState = campaign.characterStates[heroDeugan]!!
			mardekState.currentHealth = 0
			deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats, deuganState.activeStatusEffects)
			startSimpleBattle(campaign)

			val battle = campaign.currentArea!!.activeBattle!!
			assertEquals(listOf(battle.allPlayers()[1]), battle.livingPlayers())
			assertEquals(2, battle.allPlayers().size)

			battle.opponents[1] = null
			assertEquals(emptyList<CombatantState>(), battle.livingOpponents())
		}
	}
}
