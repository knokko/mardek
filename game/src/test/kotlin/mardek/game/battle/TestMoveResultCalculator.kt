package mardek.game.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.skill.ReactionSkillType
import mardek.content.stats.CombatStat
import mardek.content.stats.StatusEffect
import mardek.game.TestingInstance
import mardek.content.battle.Enemy
import mardek.state.ingame.battle.MoveResultCalculator
import org.junit.jupiter.api.Assertions.*

object TestMoveResultCalculator {

	fun testSimpleElementCreatureStunCrit(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			run {
				val mardek = campaign.characterStates[instance.heroMardek]!!
				val quarryBeast = instance.content.skills.reactionSkills.find { it.name == "Quarry: BEAST" }!!
				val stunStrike = instance.content.skills.reactionSkills.find { it.name == "Stunstrike" }!!
				mardek.toggledSkills.add(quarryBeast)
				mardek.toggledSkills.add(stunStrike)
				mardek.currentLevel = 50
			}
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "monster" }!!, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			var critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], false
				)
				assertEquals(1, result.targets.size)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				assertSame(result.element, content.stats.elements.find { it.properName == "LIGHT" }!!)
				assertEquals(emptySet<StatusEffect>(), entry.addedEffects)
				if (entry.criticalHit) {
					critCounter += 1
					assertTrue(entry.damage in 9000 until 13000, "Expected ${entry.damage} to be 11000")
				} else {
					assertTrue(entry.damage in 4000 until 7000, "Expected ${entry.damage} to be 5500")
				}
			}

			assertTrue(critCounter in 800 until 1200, "Expected $critCounter to be 1000")

			critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertEquals(1, result.targets.size)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				assertSame(result.element, content.stats.elements.find { it.properName == "LIGHT" }!!)
				assertEquals(setOf(content.stats.statusEffects.find { it.niceName == "Paralysis" }!!), entry.addedEffects)
				if (entry.criticalHit) {
					critCounter += 1
					assertTrue(entry.damage in 14000 until 19000, "Expected ${entry.damage} to be 16500")
				} else {
					assertTrue(entry.damage in 6500 until 10000, "Expected ${entry.damage} to be 8250")
				}
			}

			assertTrue(critCounter in 800 until 1200, "Expected $critCounter to be 1000")
		}
	}

	fun testStrengthAndAttack(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val equipment = campaign.characterStates[instance.heroMardek]!!.equipment
			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 30
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			var critCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				assertEquals("NONE", result.element.rawName)
				if (entry.criticalHit) critCounter += 1
				else {
					assertEquals(listOf(content.audio.fixedEffects.battle.punch), result.sounds)
					assertTrue(entry.damage in 800 until 1300, "Expected ${entry.damage} to be 1050")
				}

			}
			assertTrue(critCounter in 200 .. 400, "Expected $critCounter to be 300")

			val monsterStrength = monster.baseStats[CombatStat.Strength]!!
			assertEquals(monsterStrength, battle.livingOpponents()[0].getStat(CombatStat.Strength, battleUpdateContext(campaign)))
			battle.livingOpponents()[0].statModifiers[CombatStat.Strength] = 2 * monsterStrength

			// Damage should be tripled
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				if (entry.criticalHit) {
					assertTrue(entry.damage in 5500 until 7500, "Expected ${entry.damage} to be 6300")
				} else {
					assertTrue(entry.damage in 2500 until 3500, "Expected ${entry.damage} to be 3150")
				}
			}

			// Hero's Coat gives 45 armor, so only 10% of damage left since monster attack is 50
			equipment[2] = content.items.items.find { it.flashName == "Hero's Coat" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 250 until 350, "Expected ${entry.damage} to be 315")
				}
			}

			// Hero's Armour gives 50 armor, so no damage is left
			equipment[3] = content.items.items.find { it.flashName == "Hero's Armour" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				assertEquals(0, entry.damage)
			}

			// When we give Mardek even more armour, the damage should stay 0
			equipment[4] = content.items.items.find { it.flashName == "Emerald Bangle" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				assertFalse(entry.missed)
				assertEquals(0, entry.damage)
			}
		}
	}

	fun testDamageReductionAndSoak(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 225 until 375, "Expected ${entry.damage} to be 300")
				}
			}

			val playerState = campaign.characterStates[heroMardek]!!
			playerState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG Soak 200" }!!)
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 25 until 175, "Expected ${entry.damage} to be 100")
				}
			}

			// Since damage soak is applied last, the damage should be 300 / 2 - 200 <= 0 -> 0
			playerState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG-50%" }!!)
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) assertEquals(0, entry.damage)
			}
		}
	}

	fun testComplexElementalModifiers(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			val targetEquipment = campaign.characterStates[heroMardek]!!.equipment
			val attackerEquipment = campaign.characterStates[heroDeugan]!!.equipment
			val fire = content.stats.elements.find { it.rawName == "FIRE" }!!
			attackerEquipment[0] = content.items.items.find { it.flashName == "Firefang" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 60 until 85, "Expected ${entry.damage} to be 72")
				}
			}

			// They should give +30% fire damage together
			attackerEquipment[2] = content.items.items.find { it.flashName == "Feather Crest" }!!
			for (index in arrayOf(4, 5)) {
				attackerEquipment[index] = content.items.items.find { it.flashName == "FireOpal" }!!
			}

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 75 until 105, "Expected ${entry.damage} to be 89")
				}
			}

			campaign.characterStates[heroMardek]!!.toggledSkills.add(
				content.skills.reactionSkills.find { it.name == "FIRE-50%" }!!
			)

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 35 until 55, "Expected ${entry.damage} to be 45")
				}
			}

			// This should give an extra 50% fire resistance, for a total of 100% resistance
			targetEquipment[4] = content.items.items.find { it.flashName == "FirePendant" }!!

			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				assertEquals(0, result.targets[0].damage)
			}

			// This should give an extra 110% fire resistance, for a total of 210%
			targetEquipment[3] = content.items.items.find { it.flashName == "Flamemail" }!!

			// To make it a nice predictable jump of 110%, compensate the armor of Flamemail
			battle.livingPlayers()[1].statModifiers[CombatStat.Attack] = targetEquipment[3]!!.equipment!!.stats.find {
				it.stat == CombatStat.MeleeDefense
			}!!.adder
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				assertSame(fire, result.element)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in -110 .. -85, "Expected ${entry.damage} to be -98")
				}
			}
		}
	}

	fun testHealthDrainAgainstUndead(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val drainHp = content.skills.reactionSkills.find { it.name == "Drain HP 10%" }!!
			val attackerState = campaign.characterStates[heroMardek]!!
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Shadowblade" }!!
			attackerState.currentLevel = 50
			attackerState.currentHealth = attackerState.determineMaxHealth(heroMardek.baseStats, emptySet())

			val paladin = content.battle.monsters.find { it.name == "fallenpaladin" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = paladin, level = 5)))

			val battle = campaign.currentArea!!.activeBattle!!
			battle.livingOpponents()[0].statModifiers[CombatStat.MeleeDefense] = 10

			val dark = content.stats.elements.find { it.rawName == "DARK" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				assertEquals(0, result.restoreAttackerHealth)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 450 .. 700, "Expected ${entry.damage} to be 561")
				}
			}

			// The drain HP skill should work fine
			attackerState.toggledSkills.add(drainHp)
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 450 .. 700, "Expected ${entry.damage} to be 561")
					assertTrue(result.restoreAttackerHealth in 45 .. 70, "Expected ${result.restoreAttackerHealth} to be 56")
				}
			}
			attackerState.toggledSkills.remove(drainHp)

			// Blood Sword has 5 attack more than Shadowblade, which we compensate
			battle.livingPlayers()[0].statModifiers[CombatStat.Attack] = -5
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Blood Sword" }!!
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				assertSame(dark, result.element)
				val entry = result.targets[0]
				assertEquals(entry.damage, result.restoreAttackerHealth)
				if (!entry.criticalHit) {
					assertTrue(entry.damage in -700 .. -450, "Expected ${entry.damage} to be -561")
				}
			}
		}
	}

	fun testRemoveStatusEffects(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekEquipment = campaign.characterStates[heroMardek]!!.equipment
			mardekEquipment[2] = content.items.items.find { it.flashName == "Cursed Beret" }!!

			val deuganState = campaign.characterStates[heroDeugan]!!
			val shieldBreakReaction = content.skills.reactionSkills.find { it.name == "Shield Break 10%" }!!
			deuganState.toggledSkills.add(shieldBreakReaction)

			val shieldEffect = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = monster, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!
			assertEquals(3, battle.livingPlayers()[0].statusEffects.size)
			assertTrue(battle.livingPlayers()[0].statusEffects.contains(shieldEffect))

			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingPlayers()[0], true
				)
				// Since Mardek has auto-shield (from Cursed Beret), Deugan cannot break his shield
				assertEquals(emptySet<StatusEffect>(), result.targets[0].removedEffects)
			}

			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingOpponents()[0], true
				)
				// The monster doesn't have a shield, so there is nothing to break
				assertEquals(emptySet<StatusEffect>(), result.targets[0].removedEffects)
			}

			battle.livingOpponents()[0].statusEffects.add(shieldEffect)
			var shieldBreakCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[1], battle.livingOpponents()[0], true
				)
				if (result.targets[0].removedEffects == setOf(shieldEffect)) shieldBreakCounter += 1
			}
			assertTrue(shieldBreakCounter in 800 .. 1200, "Expected $shieldBreakCounter to be 1000")
		}
	}

	fun testAddSleepAfterRemoveSleep(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val sleepEffect = content.stats.statusEffects.find { it.niceName == "Sleep" }!!
			val dreamFish = content.battle.monsters.find { it.name == "dreamfish" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = dreamFish, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			// Dreamfish attacks have 100% chance to apply sleep
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(emptySet<StatusEffect>(), result.targets[0].removedEffects)
				assertEquals(setOf(sleepEffect), result.targets[0].addedEffects)
			}

			battle.livingPlayers()[0].statusEffects.add(sleepEffect)
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)

				// Basic attacks remove sleep, but Dreamfish attacks also add sleep
				assertEquals(setOf(sleepEffect), result.targets[0].removedEffects)
				assertEquals(setOf(sleepEffect), result.targets[0].addedEffects)
			}

			// Give Mardek 2 * 20% = 40% sleep resistance
			val bodyCrystal = content.items.items.find { it.flashName == "Body Crystal" }!!
			val equipment = campaign.characterStates[heroMardek]!!.equipment
			equipment[4] = bodyCrystal
			equipment[5] = bodyCrystal

			var sleepCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)

				// Basic attacks remove sleep, but Dreamfish attacks also add sleep
				assertEquals(setOf(sleepEffect), result.targets[0].removedEffects)
				if (result.targets[0].addedEffects.contains(sleepEffect)) sleepCounter += 1
			}

			assertTrue(sleepCounter in 5500 .. 6500, "Expected $sleepCounter to be 6000")
		}
	}

	fun testDrainMana(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			// Give Mardek a wand and a lot of mana
			val attackerState = campaign.characterStates[heroMardek]!!
			attackerState.equipment[0] = content.items.items.find { it.flashName == "Water Rod" }!!
			attackerState.equipment[2] = content.items.items.find { it.flashName == "Silver Circlet" }!!
			attackerState.equipment[3] = content.items.items.find { it.flashName == "Turquoise Armour" }!!
			attackerState.equipment[4] = content.items.items.find { it.flashName == "Sapphire Bangle" }!!
			attackerState.equipment[5] = content.items.items.find { it.flashName == "Sapphire Bangle" }!!
			attackerState.currentLevel = 50
			attackerState.currentHealth = attackerState.determineMaxHealth(heroMardek.baseStats, emptySet())
			attackerState.currentMana = attackerState.determineMaxMana(heroMardek.baseStats, emptySet())

			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 50), null, null,
				Enemy(monster = content.battle.monsters.find { it.name == "brinary" }!!, level = 50)
			))

			val battle = campaign.currentArea!!.activeBattle!!

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				val entry = result.targets[0]
				assertEquals(entry.damage, result.restoreAttackerMana)
				assertEquals(0, entry.overrideBlinkColor)
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 120 .. 160, "Expected ${entry.damage} to be 138")
				}
			}

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[1], true
				)
				val entry = result.targets[0]
				assertEquals(entry.damage, result.restoreAttackerMana)
				if (!entry.criticalHit) {
					assertTrue(entry.damage in -160 .. -120, "Expected ${entry.damage} to be -137")
				}
			}
		}
	}

	fun testSurvivor(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val survivor = content.skills.reactionSkills.find { it.survivor && it.type == ReactionSkillType.MeleeDefense }!!
			campaign.characterStates[heroMardek]!!.toggledSkills.add(survivor)

			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 100), null, null, null
			))

			val battle = campaign.currentArea!!.activeBattle!!

			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(result.targets[0].damage, battle.livingPlayers()[0].currentHealth - 1)
			}

			battle.livingPlayers()[0].currentHealth = 1
			repeat(100) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 2800..3500, "Expected ${entry.damage} to be 3150")
				}
			}
		}
	}

	fun testEvasion(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.equipment[0] = content.items.items.find { it.flashName == "Shadowblade" }!!

			val dreamFish = content.battle.monsters.find { it.name == "dreamfish" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = dreamFish, level = 5
			)))
			val battle = campaign.currentArea!!.activeBattle!!

			// Dreamfish has 50% evasion and Shadowblade has 97% accuracy
			var missCounter = 0
			repeat(100_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (result.targets[0].missed) missCounter += 1
			}

			assertTrue(missCounter in 51_000 .. 55_000, "Expected $missCounter to be 53000")

			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Accuracy+50%" }!!)

			// Now Mardek should get enough accuracy
			missCounter = 0
			repeat(100_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				if (result.targets[0].missed) missCounter += 1
			}

			assertTrue(missCounter in 2000 .. 4000, "Expected $missCounter to be 3000")

			// Let's reverse the roles and give Mardek some evasion
			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Evasion 20%" }!!)

			missCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				if (result.targets[0].missed) missCounter += 1
			}
			assertTrue(missCounter in 1000 .. 3000, "Expected $missCounter to be 2000")

			// When Mardek does not pass the challenge, he should always get hit
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], false
				)
				assertFalse(result.targets[0].missed)
			}
		}
	}

	fun testShieldAndBerserkDamage(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val shieldEffect = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val berserkEffect = content.stats.statusEffects.find { it.niceName == "Berserk" }!!

			val smith = content.battle.monsters.find { it.name == "zombielocksmith" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = smith, level = 5)))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 200..300, "Expected ${entry.damage} to be 250")
				}
			}

			battle.livingPlayers()[0].statusEffects.add(berserkEffect)
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 400..600, "Expected ${entry.damage} to be 500")
				}
			}

			battle.livingOpponents()[0].statusEffects.remove(shieldEffect)
			repeat(10_000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeBasicAttackResult(
					battle.livingPlayers()[0], battle.livingOpponents()[0], true
				)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 800..1200, "Expected ${entry.damage} to be 1000")
				}
			}
		}
	}

	fun testDamageReductionDoesNotHeal(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Nullify Physical" }!!)
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "DMG Soak 200" }!!)
			deuganState.equipment[2] = content.items.items.find { it.flashName == "Hero's Coat" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 1)))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(100) {
				val mardekHit = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], false
				)
				assertTrue(
					mardekHit.targets[0].damage > 0,
					"Expected ${mardekHit.targets[0].damage} to be positive"
				)

				val mardekBlock = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[0], true
				)
				assertEquals(0, mardekBlock.targets[0].damage)

				val deuganHit = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[1], false
				)
				assertTrue(
					deuganHit.targets[0].damage > 0,
					"Expected ${deuganHit.targets[0].damage} to be positive"
				)

				val deuganBlock = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeBasicAttackResult(
					battle.livingOpponents()[0], battle.livingPlayers()[1], true
				)
				assertEquals(0, deuganBlock.targets[0].damage)
			}
		}
	}

	fun testShockDamageAndParalyze(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 1)))
			val battle = campaign.currentArea!!.activeBattle!!

			var stunCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					shock, battle.livingPlayers()[0],
					arrayOf(battle.livingOpponents()[0]), false
				)
				assertSame(content.stats.elements.find { it.rawName == "AIR" }!!, result.element)
				assertEquals(1, result.targets.size)
				val entry = result.targets[0]
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 4500..6500, "Expected ${entry.damage} to be 5500")
				}
				if (entry.addedEffects.isNotEmpty()) {
					assertEquals(setOf(content.stats.statusEffects.find { it.flashName == "PAR" }!!), entry.addedEffects)
					stunCounter += 1
				}
			}

			assertTrue(stunCounter in 500 .. 1500, "Expected $stunCounter to be 1000")
		}
	}

	fun testDarkClawBlinding(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50
			mardekState.equipment[1] = content.items.items.find { it.flashName == "Hero's Shield" }!!
			mardekState.equipment[3] = content.items.items.find { it.flashName == "Hero's Armour" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			val darkClaw = monster.actions.find { it.name == "Dark Claw" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 30)))
			val battle = campaign.currentArea!!.activeBattle!!

			var blindCounter = 0
			repeat(10_000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					darkClaw, battle.livingOpponents()[0],
					arrayOf(battle.livingPlayers()[0]), false
				)
				assertSame(content.stats.elements.find { it.rawName == "DARK" }!!, result.element)
				val entry = result.targets[0]
				assertSame(result.element, entry.element)
				assertEquals(0, entry.overrideBlinkColor)
				if (!entry.criticalHit) {
					assertTrue(entry.damage in 300..450, "Expected ${entry.damage} to be 378")
				}
				if (entry.addedEffects.isNotEmpty()) {
					assertEquals(setOf(content.stats.statusEffects.find { it.flashName == "DRK" }!!), entry.addedEffects)
					blindCounter += 1
				}
			}

			assertTrue(blindCounter in 500 .. 1500, "Expected $blindCounter to be 1000")
		}
	}

	fun testThousandNeedles(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.currentLevel = 50
			mardekState.equipment[3] = content.items.items.find { it.flashName == "Hero's Armour" }!!

			val johnny = content.battle.monsters.find { it.name == "happyjohnny" }!!
			val mimicry = content.skills.classes.find { it.name == "Mimicry" }!!
			val thousandNeedles = mimicry.actions.find { it.name == "1000 Needles" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = johnny, level = 30)))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(10_000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					thousandNeedles, battle.livingOpponents()[0],
					arrayOf(battle.livingPlayers()[0]), false
				)
				assertSame(content.stats.elements.find { it.rawName == "NONE" }!!, result.element)
				assertEquals(1, result.targets.size)
				assertSame(result.element, result.targets[0].element)
				assertEquals(1000, result.targets[0].damage)
				assertEquals(0, result.targets[0].overrideBlinkColor)
			}
		}
	}

	fun testPyromagia(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.currentLevel = 50
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "M DMG+30%" }!!)
			deuganState.equipment[0] = content.items.items.find { it.flashName == "Balmung" }!!
			deuganState.equipment[3] = content.items.items.find { it.flashName == "Hero's Coat" }!!
			deuganState.equipment[4] = content.items.items.find { it.flashName == "Dragon Amulet" }!!

			val pyromagia = heroDeugan.characterClass.skillClass.actions.find { it.name == "Pyromagia" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 1)))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					pyromagia, battle.livingPlayers()[1],
					arrayOf(battle.livingOpponents()[0]), false
				)
				assertSame(content.stats.elements.find { it.rawName == "FIRE" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in 1800..3500, "Expected ${entry.damage} to be 2640")
				assertEquals(0, entry.addedEffects.size)
			}

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					pyromagia, battle.livingPlayers()[1],
					arrayOf(battle.livingOpponents()[0]), true
				)
				assertSame(content.stats.elements.find { it.rawName == "FIRE" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in 2300..4500, "Expected ${entry.damage} to be 3432")
				assertEquals(0, entry.addedEffects.size)
			}
		}
	}

	fun testRecover(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!
			val regeneration = content.stats.statusEffects.find { it.flashName == "RGN" }!!

			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.currentLevel = 50
			deuganState.activeStatusEffects.add(poison)
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "M DMG+30%" }!!)
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "Nullify Magic" }!!)
			deuganState.equipment[0] = content.items.items.find { it.flashName == "Balmung" }!!

			val recover = heroDeugan.characterClass.skillClass.actions.find { it.name == "Recover" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(monster = monster, level = 1)))

			val battle = campaign.currentArea!!.activeBattle!!
			val deuganCombat = battle.livingPlayers()[1]
			deuganCombat.statusEffects.add(paralysis)
			deuganCombat.statusEffects.add(regeneration)

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(recover, deuganCombat, arrayOf(deuganCombat), false)
				assertSame(content.stats.elements.find { it.rawName == "LIGHT" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in -180000..-90000, "Expected ${entry.damage} to be -132000")
				assertEquals(0, entry.addedEffects.size)
				assertEquals(setOf(poison, paralysis), entry.removedEffects)
			}

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(recover, deuganCombat, arrayOf(deuganCombat), true)
				assertSame(content.stats.elements.find { it.rawName == "LIGHT" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in -230000..-110000, "Expected ${entry.damage} to be -170000")
				assertEquals(0, entry.addedEffects.size)
				assertEquals(setOf(poison, paralysis), entry.removedEffects)
			}

			// Increasing magic resistance should NOT decrease healing power
			deuganState.equipment[3] = content.items.items.find { it.flashName == "Hero's Shield" }!!
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "M DMG-30%" }!!)

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(recover, deuganCombat, arrayOf(deuganCombat), true)
				assertSame(content.stats.elements.find { it.rawName == "LIGHT" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in -230000..-110000, "Expected ${entry.damage} to be -170000")
				assertEquals(0, entry.addedEffects.size)
				assertEquals(setOf(poison, paralysis), entry.removedEffects)
			}
		}
	}

	fun testMultiTargetFrostasia(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.currentLevel = 50
			deuganState.toggledSkills.add(content.skills.reactionSkills.find { it.name == "M DMG+30%" }!!)
			deuganState.equipment[0] = content.items.items.find { it.flashName == "Balmung" }!!
			deuganState.equipment[3] = content.items.items.find { it.flashName == "Hero's Coat" }!!
			deuganState.equipment[4] = content.items.items.find { it.flashName == "Dragon Amulet" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = monster, level = 10), null, null, Enemy(monster = monster, level = 1))
			)
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					frostasia, battle.livingPlayers()[1],
					battle.livingOpponents().toTypedArray(), false
				)
				assertSame(content.stats.elements.find { it.rawName == "WATER" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in 900..1800, "Expected ${entry.damage} to be 1320")
				assertEquals(0, entry.addedEffects.size)
			}

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					frostasia, battle.livingPlayers()[1],
					battle.livingOpponents().toTypedArray(), true
				)
				assertSame(content.stats.elements.find { it.rawName == "WATER" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertTrue(entry.damage in 1100..2300, "Expected ${entry.damage} to be 1716")
				assertEquals(0, entry.addedEffects.size)
			}
		}
	}

	fun testRageChord(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val skeleton = content.battle.monsters.find { it.name == "charredbones" }!!
			val rageChord = skeleton.strategies[0].entries.find { it.chance == 10 }!!.skill!!
			val berserk = content.stats.statusEffects.find { it.flashName == "BSK" }!!

			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = skeleton, level = 10), null, null, null
			))
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					rageChord, battle.livingOpponents()[0],
					arrayOf(battle.livingPlayers()[1]), true
				)
				assertSame(content.stats.elements.find { it.rawName == "FIRE" }!!, result.element)
				val entry = result.targets[0]
				assertFalse(entry.criticalHit)
				assertEquals(0, entry.damage)
				assertFalse(entry.missed)
				assertEquals(0, entry.addedStatModifiers.size)
				assertEquals(0, entry.removedEffects.size)
				assertEquals(setOf(berserk), entry.addedEffects)
			}

			// Let's give Deugan berserk, so that he can't get it again
			battle.livingPlayers()[1].statusEffects.add(berserk)
			repeat(1000) {
				val result = MoveResultCalculator(
					battleUpdateContext(campaign)
				).computeSkillResult(
					rageChord, battle.livingOpponents()[0],
					arrayOf(battle.livingPlayers()[1]), true
				)
				assertSame(content.stats.elements.find { it.rawName == "FIRE" }!!, result.element)
				val entry = result.targets[0]
				assertEquals(0, entry.addedEffects.size)
			}
		}
	}

	fun testPotion(instance: TestingInstance) {
		instance.apply {
			val potion = content.items.items.find { it.flashName == "Potion" }!!
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					potion, battle.livingPlayers()[0], battle.livingPlayers()[0]
				)
				assertEquals("LIGHT", result.element.rawName)
				assertEquals(0, result.restoreAttackerHealth)
				assertEquals(0, result.restoreAttackerMana)
				assertEquals(1, result.targets.size)
				assertEquals(1, result.sounds.size)

				val entry = result.targets[0]
				assertEquals("LIGHT", entry.element.rawName)
				assertEquals(rgb(100, 160, 220), entry.overrideBlinkColor)
				assertFalse(entry.criticalHit)
				assertFalse(entry.missed)
				assertEquals(-100, entry.damage)
				assertEquals(0, entry.damageMana)
				assertEquals(0, entry.addedEffects.size)
				assertEquals(0, entry.removedEffects.size)
				assertEquals(0, entry.addedStatModifiers.size)
			}
		}
	}

	fun testEther(instance: TestingInstance) {
		instance.apply {
			val ether = content.items.items.find { it.flashName == "Ether" }!!
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					ether, battle.livingPlayers()[0], battle.livingPlayers()[1]
				)
				assertEquals("LIGHT", result.element.properName)
				assertEquals(0, result.restoreAttackerHealth)
				assertEquals(0, result.restoreAttackerMana)
				assertEquals(1, result.targets.size)
				assertEquals(1, result.sounds.size)

				val entry = result.targets[0]
				assertEquals("LIGHT", entry.element.properName)
				assertEquals(rgb(100, 255, 255), entry.overrideBlinkColor)
				assertFalse(entry.criticalHit)
				assertFalse(entry.missed)
				assertEquals(0, entry.damage)
				assertEquals(-100, entry.damageMana)
				assertEquals(0, entry.addedEffects.size)
				assertEquals(0, entry.removedEffects.size)
				assertEquals(0, entry.addedStatModifiers.size)
			}
		}
	}

	fun testElixir(instance: TestingInstance) {
		instance.apply {
			val elixir = content.items.items.find { it.flashName == "Elixir" }!!
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			val combatDeugan = battle.livingPlayers()[1]
			combatDeugan.currentHealth = 200
			combatDeugan.maxHealth = 350
			combatDeugan.currentMana = 50
			combatDeugan.maxMana = 120

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					elixir, battle.livingPlayers()[1], battle.livingPlayers()[1]
				)
				assertEquals("EARTH", result.element.rawName)
				assertEquals(0, result.restoreAttackerHealth)
				assertEquals(0, result.restoreAttackerMana)
				assertEquals(1, result.targets.size)
				assertEquals(1, result.sounds.size)

				val entry = result.targets[0]
				assertEquals(-150, entry.damage)
				assertEquals(-70, entry.damageMana)
			}
		}
	}

	fun testElixirOnSkeleton(instance: TestingInstance) {
		instance.apply {
			val skeleton = content.battle.monsters.find { it.name == "skeleton" }!!
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign, arrayOf(null, null, Enemy(skeleton, 10), null))
			val battle = campaign.currentArea!!.activeBattle!!

			val combatSkeleton = battle.livingOpponents()[0]
			combatSkeleton.currentHealth = 200
			combatSkeleton.maxHealth = 350
			combatSkeleton.currentMana = 50
			combatSkeleton.maxMana = 120

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					elixir, battle.livingPlayers()[0], combatSkeleton
				)
				assertSame(content.stats.elements.find { it.rawName == "LIGHT" }!!, result.element)
				assertEquals(0, result.restoreAttackerHealth)
				assertEquals(0, result.restoreAttackerMana)
				assertEquals(1, result.targets.size)
				assertEquals(1, result.sounds.size)

				val entry = result.targets[0]
				assertEquals(0, entry.damage)
				assertEquals(-70, entry.damageMana)
			}
		}
	}

	fun testPhoenixDown(instance: TestingInstance) {
		instance.apply {
			val phoenixDown = content.items.items.find { it.flashName == "PhoenixDown" }!!
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.currentHealth = 0
			combatMardek.maxHealth = 400

			repeat(1000) {
				val mardekResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					phoenixDown, combatMardek, combatMardek
				)
				val deuganResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					phoenixDown, combatMardek, battle.allPlayers()[1]
				)

				val mardekEntry = mardekResult.targets[0]
				assertEquals(-200, mardekEntry.damage)

				// Deugan is not K.O.
				val deuganEntry = deuganResult.targets[0]
				assertEquals(0, deuganEntry.damage)
			}
		}
	}

	fun testAntidote(instance: TestingInstance) {
		instance.apply {
			val antidote = content.items.items.find { it.flashName == "Antidote" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!

			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.statusEffects.add(poison)
			combatMardek.statusEffects.add(paralysis)

			val combatDeugan = battle.livingPlayers()[1]
			combatDeugan.statusEffects.add(paralysis)

			repeat(1000) {
				val mardekResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					antidote, combatMardek, combatMardek
				)
				val deuganResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					antidote, combatDeugan, battle.allPlayers()[1]
				)

				val mardekEntry = mardekResult.targets[0]
				assertEquals(0, mardekEntry.damage)
				assertEquals(setOf(poison), mardekEntry.removedEffects)
				assertEquals(0, mardekEntry.addedEffects.size)

				// Note that only Mardek is currently poisoned
				val deuganEntry = deuganResult.targets[0]
				assertEquals(0, deuganEntry.damage)
				assertEquals(0, deuganEntry.removedEffects.size)
				assertEquals(0, deuganEntry.addedEffects.size)
			}
		}
	}

	fun testRemedy(instance: TestingInstance) {
		instance.apply {
			val remedy = content.items.items.find { it.flashName == "Remedy" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!
			val shield = content.stats.statusEffects.find { it.flashName == "PSH" }!!

			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.statusEffects.add(poison)
			combatMardek.statusEffects.add(paralysis)
			combatMardek.statusEffects.add(shield)

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					remedy, combatMardek, combatMardek
				)

				val entry = result.targets[0]
				assertEquals(0, entry.damage)
				assertEquals(setOf(poison, paralysis), entry.removedEffects)
				assertEquals(0, entry.addedEffects.size)
			}
		}
	}

	fun testAngryJuice(instance: TestingInstance) {
		instance.apply {
			val angryJuice = content.items.items.find { it.flashName == "Angry Juice" }!!
			val berserk = content.stats.statusEffects.find { it.flashName == "BSK" }!!

			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			val combatMardek = battle.livingPlayers()[0]
			combatMardek.statusEffects.add(berserk)

			repeat(1000) {
				val mardekResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					angryJuice, combatMardek, combatMardek
				)
				val deuganResult = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					angryJuice, combatMardek, battle.allPlayers()[1]
				)

				// Note that Mardek already has Berserk
				val mardekEntry = mardekResult.targets[0]
				assertEquals(0, mardekEntry.damage)
				assertEquals(0, mardekEntry.removedEffects.size)
				assertEquals(0, mardekEntry.addedEffects.size)

				val deuganEntry = deuganResult.targets[0]
				assertEquals(0, deuganEntry.damage)
				assertEquals(0, deuganEntry.removedEffects.size)
				assertEquals(setOf(berserk), deuganEntry.addedEffects)
			}
		}
	}

	fun testMagicDrink(instance: TestingInstance) {
		instance.apply {
			val magicDrink = content.items.items.find { it.flashName == "Magic Drink" }!!

			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = campaign.currentArea!!.activeBattle!!

			repeat(1000) {
				val result = MoveResultCalculator(battleUpdateContext(campaign)).computeItemResult(
					magicDrink, battle.livingOpponents()[0], battle.livingPlayers()[1]
				)

				val entry = result.targets[0]
				assertEquals(-200, entry.damageMana)
				assertEquals(0, entry.removedEffects.size)
				assertEquals(0, entry.addedEffects.size)
				assertEquals(1, entry.addedStatModifiers.size)
				val increaseSpirit = entry.addedStatModifiers[CombatStat.Spirit]
				assertEquals(10, increaseSpirit)
			}
		}
	}
}
