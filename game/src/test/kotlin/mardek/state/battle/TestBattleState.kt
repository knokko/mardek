package mardek.state.battle

import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.CombatantReference
import org.junit.jupiter.api.Assertions.assertEquals

object TestBattleState {

	fun testListPlayersAndEnemies(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(content, CampaignState(
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
			val mardekReference = CombatantReference(isPlayer = true, index = 0, battle)
			val deuganReference = CombatantReference(isPlayer = true, index = 2, battle)
			val monsterReference = CombatantReference(isPlayer = false, index = 1, battle)

			assertEquals(listOf(mardekReference, deuganReference), battle.allPlayers())
			assertEquals(listOf(deuganReference), battle.livingPlayers())
			assertEquals(listOf(monsterReference), battle.livingEnemies())

			battle.enemyStates[1] = null
			assertEquals(emptyList<CombatantReference>(), battle.livingEnemies())
		}
	}
}
