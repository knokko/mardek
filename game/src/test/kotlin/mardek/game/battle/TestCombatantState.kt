package mardek.game.battle

import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.content.battle.Enemy
import mardek.state.ingame.area.AreaSuspensionBattle
import org.junit.jupiter.api.Assertions.assertEquals

object TestCombatantState {

	fun testGetStat(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[4]] = content.items.items.find { it.displayName == "Boots of Celerity" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "AGL+1" }!!)

			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val mardekCombatState = battle.livingPlayers()[0]
			mardekCombatState.statModifiers[CombatStat.Agility] = 12
			mardekCombatState.statusEffects.add(content.stats.statusEffects.find { it.niceName == "Barskin" }!!)

			// 12 base agility
			// + 10 agility from boots of celerity
			// + 1 agility from passive
			// + 12 agility from combat stat modifiers
			assertEquals(35, battle.allPlayers()[0].getStat(CombatStat.Agility, battleUpdateContext(campaign)))

			val monsterState = battle.livingOpponents()[0]
			monsterState.statModifiers[CombatStat.Agility] = -5
			// 8 base agility minus 5
			assertEquals(3, monsterState.getStat(CombatStat.Agility, battleUpdateContext(campaign)))
		}
	}

	fun testGetElementalResistance(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[4]] = content.items.items.find { it.displayName == "FirePendant" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Resist FIRE" }!!)

			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val mardekCombatState = battle.livingPlayers()[0]
			val astralForm = content.stats.statusEffects.find { it.niceName == "Astral" }!!
			mardekCombatState.statusEffects.add(astralForm)

			// 50% resistance from fire pendant
			// 50% resistance from resist fire skill
			// 50% resistance from astral form
			val fire = content.stats.elements.find { it.properName == "FIRE" }!!
			assertEquals(1.5f, battle.allPlayers()[0].getResistance(fire, battleUpdateContext(campaign)))

			// Note that Astral Form reduces light & dark resistance by 100%
			val light = content.stats.elements.find { it.properName == "LIGHT" }!!
			assertEquals(-0.8f, battle.allPlayers()[0].getResistance(light, battleUpdateContext(campaign)), 0.01f)
			val dark = content.stats.elements.find { it.properName == "DARK" }!!
			assertEquals(-1.2f, battle.allPlayers()[0].getResistance(dark, battleUpdateContext(campaign)), 0.01f)

			val monsterState = battle.livingOpponents()[0]
			monsterState.statusEffects.add(astralForm)

			// -100% base light resistance
			// -100% light resistance from astral form
			assertEquals(-2f, monsterState.getResistance(light, battleUpdateContext(campaign)))
		}
	}

	fun testGetEffectResistance(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[4]] = content.items.items.find { it.displayName == "Amethyst" }!!
			mardekState.toggledSkills.add(content.skills.passiveSkills.find { it.name == "Antibody" }!!)

			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "kdestralan_mind" }!!, level = 5
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val confusion = content.stats.statusEffects.find { it.niceName == "Confusion" }!!
			val poison = content.stats.statusEffects.find { it.niceName == "Poison" }!!

			assertEquals(100, battle.allPlayers()[0].getResistance(poison, battleUpdateContext(campaign)))
			assertEquals(30, battle.allPlayers()[0].getResistance(confusion, battleUpdateContext(campaign)))
			assertEquals(100, battle.livingOpponents()[0].getResistance(poison, battleUpdateContext(campaign)))
		}
	}

	fun testTransferStatusBack(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val mardekState = campaign.characterStates[heroMardek]!!
			val deuganState = campaign.characterStates[heroDeugan]!!
			val oldMaxHealth = mardekState.determineMaxHealth(heroMardek.baseStats, emptySet())
			val oldMaxMana = deuganState.determineMaxMana(heroDeugan.baseStats, emptySet())

			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "monster" }!!, level = 5
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val confusion = content.stats.statusEffects.find { it.niceName == "Confusion" }!!
			val poison = content.stats.statusEffects.find { it.niceName == "Poison" }!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.statusEffects.add(poison)
			combatMardek.statusEffects.add(confusion)
			combatMardek.statModifiers[CombatStat.Vitality] = 50
			combatMardek.clampHealthAndMana(battleUpdateContext(campaign))
			combatMardek.currentHealth = combatMardek.maxHealth
			combatMardek.currentMana = 0
			combatMardek.transferStatusBack(battleUpdateContext(campaign))

			assertEquals(oldMaxHealth, combatMardek.currentHealth)
			assertEquals(oldMaxHealth, combatMardek.maxHealth)
			assertEquals(oldMaxHealth, mardekState.currentHealth)
			assertEquals(setOf(poison), mardekState.activeStatusEffects)
			assertEquals(setOf(poison), combatMardek.statusEffects)
			assertEquals(0, combatMardek.currentMana)
			assertEquals(0, mardekState.currentMana)

			val combatDeugan = battle.livingPlayers()[1]
			combatDeugan.statModifiers[CombatStat.Spirit] = 50
			combatDeugan.clampHealthAndMana(battleUpdateContext(campaign))
			combatDeugan.currentMana = combatDeugan.maxMana
			combatDeugan.currentHealth = 0
			combatDeugan.transferStatusBack(battleUpdateContext(campaign))

			assertEquals(oldMaxMana, combatDeugan.currentMana)
			assertEquals(oldMaxMana, combatDeugan.maxMana)
			assertEquals(oldMaxMana, deuganState.currentMana)
			assertEquals(0, combatDeugan.currentHealth)
			assertEquals(1, deuganState.currentHealth)
		}
	}
}
