package mardek.state.battle

import mardek.content.skill.ReactionSkillType
import mardek.content.stats.CombatStat
import mardek.content.stats.StatusEffect
import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.Enemy
import mardek.state.ingame.battle.MoveResultCalculator
import org.junit.jupiter.api.Assertions.*

object TestMoveResultCalculator {

	fun testSimpleElementCreatureStunCrit(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))
			run {
				val mardek = state.campaign.characterStates[instance.heroMardek]!!
				val quarryBeast = instance.content.skills.reactionSkills.find { it.name == "Quarry: BEAST" }!!
				val stunStrike = instance.content.skills.reactionSkills.find { it.name == "Stunstrike" }!!
				mardek.toggledSkills.add(quarryBeast)
				mardek.toggledSkills.add(stunStrike)
				mardek.currentLevel = 50
			}
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "monster" }!!, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[0]

			var critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], false
				)
				assertFalse(result.missed)
				assertSame(result.element, content.stats.elements.find { it.properName == "LIGHT" }!!)
				assertEquals(emptySet<StatusEffect>(), result.addedEffects)
				if (result.criticalHit) {
					critCounter += 1
					assertTrue(result.damage in 9000 until 13000, "Expected ${result.damage} to be 11000")
				} else {
					assertTrue(result.damage in 4000 until 7000, "Expected ${result.damage} to be 5500")
				}
			}

			assertTrue(critCounter in 800 until 1200, "Expected $critCounter to be 1000")

			critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertFalse(result.missed)
				assertSame(result.element, content.stats.elements.find { it.properName == "LIGHT" }!!)
				assertEquals(setOf(content.stats.statusEffects.find { it.niceName == "Paralysis" }!!), result.addedEffects)
				if (result.criticalHit) {
					critCounter += 1
					assertTrue(result.damage in 14000 until 19000, "Expected ${result.damage} to be 16500")
				} else {
					assertTrue(result.damage in 6500 until 10000, "Expected ${result.damage} to be 8250")
				}
			}

			assertTrue(critCounter in 800 until 1200, "Expected $critCounter to be 1000")
		}
	}

	fun testStrengthAndAttack(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val equipment = state.campaign.characterStates[instance.heroMardek]!!.equipment
			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 30
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			var critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertFalse(result.missed)
				assertEquals("NONE", result.element.rawName)
				if (result.criticalHit) critCounter += 1
				else {
					assertSame(content.audio.fixedEffects.battle.punch, result.sound)
					assertTrue(result.damage in 800 until 1300, "Expected ${result.damage} to be 1050")
				}

			}
			assertTrue(critCounter in 200 .. 400, "Expected $critCounter to be 300")

			val monsterStrength = monster.baseStats[CombatStat.Strength]!!
			assertEquals(monsterStrength, battle.livingOpponents()[0].getStat(CombatStat.Strength, battleUpdateContext(state.campaign)))
			battle.livingOpponents()[0].statModifiers[CombatStat.Strength] = 2 * monsterStrength

			// Damage should be tripled
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertFalse(result.missed)
				if (result.criticalHit) {
					assertTrue(result.damage in 5500 until 7500, "Expected ${result.damage} to be 6300")
				} else {
					assertTrue(result.damage in 2500 until 3500, "Expected ${result.damage} to be 3150")
				}
			}

			// Hero's Coat gives 45 armor, so only 10% of damage left since monster attack is 50
			equipment[2] = content.items.items.find { it.flashName == "Hero's Coat" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertFalse(result.missed)
				if (!result.criticalHit) {
					assertTrue(result.damage in 250 until 350, "Expected ${result.damage} to be 315")
				}
			}

			// Hero's Armour gives 50 armor, so no damage is left
			equipment[3] = content.items.items.find { it.flashName == "Hero's Armour" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertFalse(result.missed)
				assertEquals(0, result.damage)
			}

			// When we give Mardek even more armour, the damage should stay 0
			equipment[4] = content.items.items.find { it.flashName == "Emerald Bangle" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertFalse(result.missed)
				assertEquals(0, result.damage)
			}
		}
	}

	fun testDamageReductionAndSoak(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 225 until 375, "Expected ${result.damage} to be 300")
				}
			}

			val playerState = state.campaign.characterStates[heroMardek]!!
			playerState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG Soak 200" }!!)
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 25 until 175, "Expected ${result.damage} to be 100")
				}
			}

			// Since damage soak is applied last, the damage should be 300 / 2 - 200 <= 0 -> 0
			playerState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG-50%" }!!)
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (!result.criticalHit) assertEquals(0, result.damage)
			}
		}
	}

	fun testComplexElementalModifiers(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[1]

			val targetEquipment = state.campaign.characterStates[heroMardek]!!.equipment
			val attackerEquipment = state.campaign.characterStates[heroDeugan]!!.equipment
			val fire = content.stats.elements.find { it.rawName == "FIRE" }!!
			attackerEquipment[0] = content.items.items.find { it.flashName == "Firefang" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				if (!result.criticalHit) {
					assertTrue(result.damage in 60 until 85, "Expected ${result.damage} to be 72")
				}
			}

			// They should give +30% fire damage together
			attackerEquipment[2] = content.items.items.find { it.flashName == "Feather Crest" }!!
			for (index in arrayOf(4, 5)) {
				attackerEquipment[index] = content.items.items.find { it.flashName == "FireOpal" }!!
			}

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				if (!result.criticalHit) {
					assertTrue(result.damage in 75 until 105, "Expected ${result.damage} to be 89")
				}
			}

			state.campaign.characterStates[heroMardek]!!.toggledSkills.add(
				content.skills.reactionSkills.find { it.name == "FIRE-50%" }!!
			)

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				if (!result.criticalHit) {
					assertTrue(result.damage in 35 until 55, "Expected ${result.damage} to be 45")
				}
			}

			// This should give an extra 50% fire resistance, for a total of 100% resistance
			targetEquipment[4] = content.items.items.find { it.flashName == "FirePendant" }!!

			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				assertEquals(0, result.damage)
			}

			// This should give an extra 110% fire resistance, for a total of 210%
			targetEquipment[3] = content.items.items.find { it.flashName == "Flamemail" }!!

			// To make it a nice predictable jump of 110%, compensate the armor of Flamemail
			battle.livingPlayers()[1].statModifiers[CombatStat.Attack] = targetEquipment[3]!!.equipment!!.stats.find {
				it.stat == CombatStat.MeleeDefense
			}!!.adder
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				if (!result.criticalHit) {
					assertTrue(result.damage in -110 .. -85, "Expected ${result.damage} to be -98")
				}
			}
		}
	}

	fun testHealthDrainAgainstUndead(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(
				CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
			)

			val drainHp = content.skills.reactionSkills.find { it.name == "Drain HP 10%" }!!
			val attackerState = state.campaign.characterStates[heroMardek]!!
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Shadowblade" }!!
			attackerState.currentLevel = 50
			attackerState.currentHealth = attackerState.determineMaxHealth(heroMardek.baseStats, emptySet())

			val paladin = content.battle.monsters.find { it.name == "FallenPaladin" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(monster = paladin, level = 5)))

			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.livingOpponents()[0].statModifiers[CombatStat.MeleeDefense] = 10
			battle.onTurn = battle.livingPlayers()[0]

			val dark = content.stats.elements.find { it.rawName == "DARK" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				assertEquals(0, result.restoreAttackerHealth)
				if (!result.criticalHit) {
					assertTrue(result.damage in 450 .. 700, "Expected ${result.damage} to be 561")
				}
			}

			// The drain HP skill should work fine
			attackerState.toggledSkills.add(drainHp)
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				if (!result.criticalHit) {
					assertTrue(result.damage in 450 .. 700, "Expected ${result.damage} to be 561")
					assertTrue(result.restoreAttackerHealth in 45 .. 70, "Expected ${result.restoreAttackerHealth} to be 56")
				}
			}
			attackerState.toggledSkills.remove(drainHp)

			// Blood Sword has 5 attack more than Shadowblade, which we compensate
			battle.livingPlayers()[0].statModifiers[CombatStat.Attack] = -5
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Blood Sword" }!!
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				assertEquals(result.damage, result.restoreAttackerHealth)
				if (!result.criticalHit) {
					assertTrue(result.damage in -700 .. -450, "Expected ${result.damage} to be -561")
				}
			}
		}
	}

	fun testRemoveStatusEffects(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekEquipment = state.campaign.characterStates[heroMardek]!!.equipment
			mardekEquipment[2] = content.items.items.find { it.flashName == "Cursed Beret" }!!

			val deuganState = state.campaign.characterStates[heroDeugan]!!
			val shieldBreakReaction = content.skills.reactionSkills.find { it.name == "Shield Break 10%" }!!
			deuganState.toggledSkills.add(shieldBreakReaction)

			val shieldEffect = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[1]
			assertEquals(3, battle.livingPlayers()[0].statusEffects.size)
			assertTrue(battle.livingPlayers()[0].statusEffects.contains(shieldEffect))

			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				// Since Mardek has auto-shield (from Cursed Beret), Deugan cannot break his shield
				assertEquals(emptySet<StatusEffect>(), result.removedEffects)
			}

			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingOpponents()[0], true
				)
				// The monster doesn't have a shield, so there is nothing to break
				assertEquals(emptySet<StatusEffect>(), result.removedEffects)
			}

			battle.livingOpponents()[0].statusEffects.add(shieldEffect)
			var shieldBreakCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingOpponents()[0], true
				)
				if (result.removedEffects == setOf(shieldEffect)) shieldBreakCounter += 1
			}
			assertTrue(shieldBreakCounter in 800 .. 1200, "Expected $shieldBreakCounter to be 1000")
		}
	}

	fun testAddSleepAfterRemoveSleep(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val sleepEffect = content.stats.statusEffects.find { it.niceName == "Sleep" }!!
			val dreamFish = content.battle.monsters.find { it.name == "Dreamfish" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = dreamFish, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			// Dreamfish attacks have 100% chance to apply sleep
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(emptySet<StatusEffect>(), result.removedEffects)
				assertEquals(setOf(sleepEffect), result.addedEffects)
			}

			battle.livingPlayers()[0].statusEffects.add(sleepEffect)
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)

				// Basic attacks remove sleep, but Dreamfish attacks also add sleep
				assertEquals(setOf(sleepEffect), result.removedEffects)
				assertEquals(setOf(sleepEffect), result.addedEffects)
			}

			// Give Mardek 2 * 20% = 40% sleep resistance
			val bodyCrystal = content.items.items.find { it.flashName == "Body Crystal" }!!
			val equipment = state.campaign.characterStates[heroMardek]!!.equipment
			equipment[4] = bodyCrystal
			equipment[5] = bodyCrystal

			var sleepCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)

				// Basic attacks remove sleep, but Dreamfish attacks also add sleep
				assertEquals(setOf(sleepEffect), result.removedEffects)
				if (result.addedEffects.contains(sleepEffect)) sleepCounter += 1
			}

			assertTrue(sleepCounter in 5500 .. 6500, "Expected $sleepCounter to be 6000")
		}
	}

	fun testDrainMana(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(
				CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
			)

			// Give Mardek a wand and a lot of mana
			val attackerState = state.campaign.characterStates[heroMardek]!!
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Water Rod" }!!
			attackerState.equipment[2] = content.items.items.find { it.flashName == "Silver Circlet" }!!
			attackerState.equipment[3] = content.items.items.find { it.flashName == "Turquoise Armour" }!!
			attackerState.equipment[4] = content.items.items.find { it.flashName == "Sapphire Bangle" }!!
			attackerState.equipment[5] = content.items.items.find { it.flashName == "Sapphire Bangle" }!!
			attackerState.currentLevel = 50
			attackerState.currentHealth = attackerState.determineMaxHealth(heroMardek.baseStats, emptySet())
			attackerState.currentMana = attackerState.determineMaxMana(heroMardek.baseStats, emptySet())

			startSimpleBattle(state, enemies = arrayOf(
				Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 50), null, null,
				Enemy(monster = content.battle.monsters.find { it.name == "Brinary" }!!, level = 50)
			))

			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[0]

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertEquals(result.damage, result.restoreAttackerMana)
				if (!result.criticalHit) {
					assertTrue(result.damage in 120 .. 160, "Expected ${result.damage} to be 138")
				}
			}

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[1], true
				)
				assertEquals(result.damage, result.restoreAttackerMana)
				if (!result.criticalHit) {
					assertTrue(result.damage in -160 .. -120, "Expected ${result.damage} to be -137")
				}
			}
		}
	}

	fun testSurvivor(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(
				CampaignState(
					currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
					characterSelection = simpleCharacterSelectionState(),
					characterStates = simpleCharacterStates(),
					gold = 123
				)
			)

			val survivor = content.skills.reactionSkills.find { it.survivor && it.type == ReactionSkillType.MeleeDefense }!!
			state.campaign.characterStates[heroMardek]!!.toggledSkills.add(survivor)

			startSimpleBattle(state, enemies = arrayOf(
				Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 100), null, null, null
			))

			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(result.damage, battle.livingPlayers()[0].currentHealth - 1)
			}

			battle.livingPlayers()[0].currentHealth = 1
			repeat(100) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 2800..3500, "Expected ${result.damage} to be 3150")
				}
			}
		}
	}

	fun testEvasion(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.equipment[0] = content.items.items.find { it.flashName == "Shadowblade" }!!

			val dreamFish = content.battle.monsters.find { it.name == "Dreamfish" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = dreamFish, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[0]

			// Dreamfish has 50% evasion and Shadowblade has 97% accuracy
			var missCounter = 0
			repeat(100_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (result.missed) missCounter += 1
			}

			assertTrue(missCounter in 51_000 .. 55_000, "Expected $missCounter to be 53000")

			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Accuracy+50%" }!!)

			// Now Mardek should get enough accuracy
			missCounter = 0
			repeat(100_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (result.missed) missCounter += 1
			}

			assertTrue(missCounter in 2000 .. 4000, "Expected $missCounter to be 3000")

			// Let's reverse the roles and give Mardek some evasion
			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Evasion 20%" }!!)

			missCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (result.missed) missCounter += 1
			}
			assertTrue(missCounter in 1000 .. 3000, "Expected $missCounter to be 2000")

			// When Mardek does not pass the challenge, he should always get hit
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], false
				)
				assertFalse(result.missed)
			}
		}
	}

	fun testShieldAndBerserkDamage(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val shieldEffect = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val berserkEffect = content.stats.statusEffects.find { it.niceName == "Berserk" }!!

			val smith = content.battle.monsters.find { it.name == "ZombieLocksmith" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(monster = smith, level = 5)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingPlayers()[0]

			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 200..300, "Expected ${result.damage} to be 250")
				}
			}

			battle.livingPlayers()[0].statusEffects.add(berserkEffect)
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 400..600, "Expected ${result.damage} to be 500")
				}
			}

			battle.livingOpponents()[0].statusEffects.remove(shieldEffect)
			repeat(10_000) {
				val result = MoveResultCalculator(battle, battleUpdateContext(state.campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (!result.criticalHit) {
					assertTrue(result.damage in 800..1200, "Expected ${result.damage} to be 1000")
				}
			}
		}
	}

	fun testDamageReductionDoesNotHeal(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Nullify Physical" }!!)
			val deuganState = state.campaign.characterStates[heroDeugan]!!
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG Soak 200" }!!)
			deuganState.equipment[2] = content.items.items.find { it.flashName == "Hero's Coat" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 1)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			repeat(100) {
				val mardekHit = MoveResultCalculator(
					battle, battleUpdateContext(state.campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], false
				)
				assertTrue(mardekHit.damage > 0, "Expected ${mardekHit.damage} to be positive")

				val mardekBlock = MoveResultCalculator(
					battle, battleUpdateContext(state.campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(0, mardekBlock.damage)

				val deuganHit = MoveResultCalculator(
					battle, battleUpdateContext(state.campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[1], false
				)
				assertTrue(deuganHit.damage > 0, "Expected ${deuganHit.damage} to be positive")

				val deuganBlock = MoveResultCalculator(
					battle, battleUpdateContext(state.campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[1], true
				)
				assertEquals(0, deuganBlock.damage)
			}
		}
	}
}
