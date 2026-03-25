package mardek.game.battle

import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.state.ingame.battle.MonsterCombatantState
import org.junit.jupiter.api.Assertions.assertEquals

object TestMonsterStats {

	fun testMonster(instance: TestingInstance) {
		instance.apply {
			val monster = MonsterCombatantState(
				monster = content.battle.monsters.find { it.name == "monster" }!!,
				level = 34,
				isOnPlayerSide = false,
				overrideDisplayName = null,
			)
			assertEquals(2124, monster.computeMaxHealth(
				battleUpdateContext(simpleCampaignState())
			))
			assertEquals(50, monster.getStat(
				CombatStat.Attack, battleUpdateContext(simpleCampaignState())
			))
		}
	}

	fun testFumeratLevel1(instance: TestingInstance) {
		instance.apply {
			val rat = MonsterCombatantState(
				monster = content.battle.monsters.find { it.name == "fumerat" }!!,
				level = 1,
				isOnPlayerSide = false,
				overrideDisplayName = null,
			)
			assertEquals(3, rat.computeMaxHealth(
				battleUpdateContext(simpleCampaignState())
			))
			assertEquals(3, rat.getStat(
				CombatStat.Attack, battleUpdateContext(simpleCampaignState())
			))
		}
	}

	fun testFunGoblinLevel1(instance: TestingInstance) {
		instance.apply {
			val goblin = MonsterCombatantState(
				monster = content.battle.monsters.find { it.name == "fungoblin" }!!,
				level = 1,
				isOnPlayerSide = false,
				overrideDisplayName = null,
			)
			assertEquals(4, goblin.computeMaxHealth(
				battleUpdateContext(simpleCampaignState())
			))
			assertEquals(3, goblin.getStat(
				CombatStat.Attack, battleUpdateContext(simpleCampaignState())
			))
		}
	}
}
