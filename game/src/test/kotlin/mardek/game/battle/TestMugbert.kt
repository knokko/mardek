package mardek.game.battle

import mardek.content.battle.Enemy
import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.MonsterCombatantState
import mardek.state.ingame.battle.MonsterStrategyCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

object TestMugbert {

	fun attackRegressionTest(instance: TestingInstance) {
		instance.apply {
			val mugbert = content.battle.monsters.find { it.name == "mugbert" }!!
			val campaign = simpleCampaignState()

			startSimpleBattle(campaign, enemies = arrayOf(null, null, Enemy(
				monster = mugbert, level = 3
			), null))
			val battle = ((campaign.state as AreaState).suspension as AreaSuspensionBattle).battle
			val attacker = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			var numPowerAttacks = 0
			repeat(1000) {
				val move = MonsterStrategyCalculator(battle, attacker, context).determineNextMove()
				val meleeMove = move as BattleStateMachine.MeleeAttack
				if (meleeMove.skill != null) {
					numPowerAttacks += 1
					assertEquals("Power Attack", meleeMove.skill!!.name)
				}
			}

			assertTrue(
				numPowerAttacks in 300 .. 700,
				"Expected $numPowerAttacks to be approximately 500"
			)

			assertEquals(0, attacker.getNatural(CombatStat.Attack))
			assertEquals(4, attacker.getStat(
				CombatStat.Attack, battleUpdateContext(campaign)
			))
		}
	}
}
