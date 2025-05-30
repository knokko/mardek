package mardek.state.battle

import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertEquals

object TestCombatantState {

	fun testGetStat(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.equipment[4] = content.items.items.find { it.flashName == "Boots of Celerity" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "AGL+1" }!!)

			startSimpleBattle(state)
			val battle = state.campaign.currentArea!!.activeBattle!!

			val mardekCombatState = battle.livingPlayers()[0]
			mardekCombatState.statModifiers[CombatStat.Agility] = 12
			mardekCombatState.statusEffects.add(content.stats.statusEffects.find { it.niceName == "Barskin" }!!)

			// 12 base agility
			// + 10 agility from boots of celerity
			// + 1 agility from passive
			// + 12 agility from combat stat modifiers
			assertEquals(35, battle.allPlayers()[0].getStat(CombatStat.Agility, battleUpdateContext(state.campaign)))

			val monsterState = battle.livingOpponents()[0]
			monsterState.statModifiers[CombatStat.Agility] = -5
			// 8 base agility minus 5
			assertEquals(3, monsterState.getStat(CombatStat.Agility, battleUpdateContext(state.campaign)))
		}
	}

	fun testGetElementalResistance(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.equipment[4] = content.items.items.find { it.flashName == "FirePendant" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Resist FIRE" }!!)

			startSimpleBattle(state)
			val battle = state.campaign.currentArea!!.activeBattle!!

			val mardekCombatState = battle.livingPlayers()[0]
			val astralForm = content.stats.statusEffects.find { it.niceName == "Astral" }!!
			mardekCombatState.statusEffects.add(astralForm)

			// 50% resistance from fire pendant
			// 50% resistance from resist fire skill
			// 50% resistance from astral form
			val fire = content.stats.elements.find { it.properName == "FIRE" }!!
			assertEquals(1.5f, battle.allPlayers()[0].getResistance(fire, battleUpdateContext(state.campaign)))

			// Note that Astral Form reduces light & dark resistance by 100%
			val light = content.stats.elements.find { it.properName == "LIGHT" }!!
			assertEquals(-0.8f, battle.allPlayers()[0].getResistance(light, battleUpdateContext(state.campaign)), 0.01f)
			val dark = content.stats.elements.find { it.properName == "DARK" }!!
			assertEquals(-1.2f, battle.allPlayers()[0].getResistance(dark, battleUpdateContext(state.campaign)), 0.01f)

			val monsterState = battle.livingOpponents()[0]
			monsterState.statusEffects.add(astralForm)

			// -100% base light resistance
			// -100% light resistance from astral form
			assertEquals(-2f, monsterState.getResistance(light, battleUpdateContext(state.campaign)))
		}
	}

	fun testGetEffectResistance(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.equipment[4] = content.items.items.find { it.flashName == "Amethyst" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Antibody" }!!)

			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "Kdestralan_Mind" }!!, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			val confusion = content.stats.statusEffects.find { it.niceName == "Confusion" }!!
			val poison = content.stats.statusEffects.find { it.niceName == "Poison" }!!

			assertEquals(100, battle.allPlayers()[0].getResistance(poison, battleUpdateContext(state.campaign)))
			assertEquals(30, battle.allPlayers()[0].getResistance(confusion, battleUpdateContext(state.campaign)))
			assertEquals(100, battle.livingOpponents()[0].getResistance(poison, battleUpdateContext(state.campaign)))
		}
	}
}
