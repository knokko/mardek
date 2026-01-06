package mardek.game.battle

import mardek.content.stats.CombatStat
import mardek.content.stats.StatusEffect
import mardek.game.TestingInstance
import mardek.state.ingame.battle.BattleStateMachine
import mardek.content.battle.Enemy
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.computeStatusEffectsBeforeTurn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

object TestTurnStatusEffects {

	fun testWithoutStatusEffects(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			repeat(100) {
				val effects = computeStatusEffectsBeforeTurn(
					battle.livingOpponents()[0], battleUpdateContext(campaign)
				)
				assertSame(battle.livingOpponents()[0], effects.combatant)
				assertNull(effects.forceMove)
				assertEquals(emptySet<StatusEffect>(), effects.removedEffects)
				assertEquals(0, effects.takeDamage.size)
			}
		}
	}

	fun testParalysis(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val monster = battle.livingOpponents()[0]
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!
			monster.statusEffects.add(paralysis)

			var skipCounter = 0
			repeat(10_000) {
				val effects = computeStatusEffectsBeforeTurn(
					monster, battleUpdateContext(campaign)
				)
				assertEquals(0, effects.takeDamage.size)
				assertEquals(0, effects.removedEffects.size)
				val forceMove = effects.forceMove
				if (forceMove != null) {
					assertInstanceOf<BattleStateMachine.Wait>(forceMove.move)
					assertSame(paralysis, forceMove.effect)
					assertEquals(paralysis.skipTurn!!.blinkColor, forceMove.blinkColor)
					assertEquals(paralysis.skipTurn!!.particleEffect, forceMove.particleEffect)
					skipCounter += 1
				}
			}

			assertTrue(skipCounter in 3500 .. 4500, "Expected $skipCounter to be 4000")
		}
	}

	fun testSleep(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val monster = battle.livingOpponents()[0]
			val sleep = content.stats.statusEffects.find { it.flashName == "SLP" }!!
			monster.statusEffects.add(sleep)

			var skipCounter = 0
			repeat(10_000) {
				val effects = computeStatusEffectsBeforeTurn(
					monster, battleUpdateContext(campaign)
				)
				assertEquals(0, effects.takeDamage.size)
				val forceMove = effects.forceMove
				if (forceMove != null) {
					assertEquals(0, effects.removedEffects.size)
					assertInstanceOf<BattleStateMachine.Wait>(forceMove.move)
					assertSame(sleep, forceMove.effect)
					assertEquals(0, forceMove.blinkColor)
					assertEquals(null, forceMove.particleEffect)
					skipCounter += 1
				} else {
					assertEquals(setOf(sleep), effects.removedEffects)
				}
			}

			assertTrue(skipCounter in 7000 .. 8000, "Expected $skipCounter to be 7500")
		}
	}

	fun testPoisonLowDamage(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val mardek = battle.livingPlayers()[0]
			mardek.statusEffects.add(poison)
			mardek.statModifiers[CombatStat.Vitality] = -10
			mardek.clampHealthAndMana(battleUpdateContext(campaign))
			assertEquals(24, mardek.maxHealth)

			repeat(100) {
				val effects = computeStatusEffectsBeforeTurn(
					mardek, battleUpdateContext(campaign)
				)
				assertSame(mardek, effects.combatant)
				assertNull(effects.forceMove)
				assertEquals(emptySet<StatusEffect>(), effects.removedEffects)
				assertEquals(1, effects.takeDamage.size)

				val takeDamage = effects.takeDamage[0]
				assertSame(poison, takeDamage.effect)

				// Poison must deal at least 3 damage, even when max. health is very low
				assertEquals(3, takeDamage.amount)
			}
		}
	}

	fun testPoisonHighDamage(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.characterStates[heroMardek]!!.currentLevel = 50
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val mardek = battle.livingPlayers()[0]
			mardek.statusEffects.add(poison)
			assertEquals(1854, mardek.maxHealth)

			repeat(100) {
				val effects = computeStatusEffectsBeforeTurn(
					mardek, battleUpdateContext(campaign)
				)
				assertSame(mardek, effects.combatant)
				assertNull(effects.forceMove)
				assertEquals(emptySet<StatusEffect>(), effects.removedEffects)
				assertEquals(1, effects.takeDamage.size)

				val takeDamage = effects.takeDamage[0]
				assertSame(poison, takeDamage.effect)

				assertEquals(93, takeDamage.amount)
			}
		}
	}

	fun testRegeneration(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val regeneration = content.stats.statusEffects.find { it.flashName == "RGN" }!!
			val mardek = battle.livingPlayers()[0]
			mardek.statusEffects.add(regeneration)
			mardek.clampHealthAndMana(battleUpdateContext(campaign))
			assertEquals(54, mardek.maxHealth)

			repeat(100) {
				val effects = computeStatusEffectsBeforeTurn(
					mardek, battleUpdateContext(campaign)
				)
				assertSame(mardek, effects.combatant)
				assertNull(effects.forceMove)
				assertEquals(emptySet<StatusEffect>(), effects.removedEffects)
				assertEquals(1, effects.takeDamage.size)

				val takeDamage = effects.takeDamage[0]
				assertSame(regeneration, takeDamage.effect)
				assertEquals(-5, takeDamage.amount)
			}
		}
	}

	fun testMultiple(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val monster = battle.livingOpponents()[0]
			assertEquals(1480, monster.maxHealth)
			assertEquals(10, monster.getLevel(battleUpdateContext(campaign)))

			val sleep = content.stats.statusEffects.find { it.flashName == "SLP" }!!
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val regeneration = content.stats.statusEffects.find { it.flashName == "RGN" }!!
			monster.statusEffects.addAll(setOf(sleep, poison, paralysis, regeneration))

			var skipCounter = 0
			repeat(10_000) {
				val effects = computeStatusEffectsBeforeTurn(
					monster, battleUpdateContext(campaign)
				)

				// Damaging status effects come before healing status effects
				assertEquals(2, effects.takeDamage.size)
				val poisonDamage = effects.takeDamage[0]
				assertSame(poison, poisonDamage.effect)
				assertEquals(74, poisonDamage.amount)
				val regenerationHealing = effects.takeDamage[1]
				assertSame(regeneration, regenerationHealing.effect)

				// Regeneration heals for 2 * level every turn
				assertEquals(-20, regenerationHealing.amount)

				val forceMove = effects.forceMove
				if (forceMove != null) {
					if (forceMove.effect === sleep) {
						assertEquals(0, effects.removedEffects.size)
						assertEquals(0, forceMove.blinkColor)
						assertNull(forceMove.particleEffect)
					} else {
						assertTrue(effects.removedEffects.isEmpty() || effects.removedEffects == setOf(sleep))
						assertSame(paralysis, forceMove.effect)
						assertNotEquals(0, forceMove.blinkColor)
						assertNotNull(forceMove.particleEffect)
					}
					assertInstanceOf<BattleStateMachine.Wait>(forceMove.move)
					skipCounter += 1
				} else {
					assertEquals(setOf(sleep), effects.removedEffects)
				}
			}

			// Sleep has 75% chance to skip turns, and paralysis has 40% chance to skip turns
			// So the chance of NO skip is 0.25 * 0.6 * 100% = 15%
			assertTrue(skipCounter in 8000 .. 9000, "Expected $skipCounter to be 8500")
		}
	}

	fun testRemoveEffectsWhenResistanceIsLarge(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				content.battle.monsters.find { it.name == "kdestralan_mind" }!!, 1
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle

			val animus = battle.livingOpponents()[0]

			val sleep = content.stats.statusEffects.find { it.flashName == "SLP" }!!
			val paralysis = content.stats.statusEffects.find { it.flashName == "PAR" }!!
			val poison = content.stats.statusEffects.find { it.flashName == "PSN" }!!
			val regeneration = content.stats.statusEffects.find { it.flashName == "RGN" }!!
			animus.statusEffects.addAll(setOf(sleep, poison, paralysis, regeneration))

			var removePoisonCounter = 0
			var removeParalysisCounter = 0
			var removeSleepCounter = 0
			var removeTwoCounter = 0
			var removeAllCounter = 0
			repeat(100_000) {
				val effects = computeStatusEffectsBeforeTurn(
					animus, battleUpdateContext(campaign)
				)

				// Animus does not have regeneration resistance, so it won't randomly 'cure' regeneration
				assertTrue(effects.removedEffects.size <= 3)

				if (effects.removedEffects.size == 2) removeTwoCounter += 1
				if (effects.removedEffects.size == 3) removeAllCounter += 1

				if (effects.removedEffects.contains(poison)) {
					assertEquals(1, effects.takeDamage.size)
					removePoisonCounter += 1
				} else {
					assertEquals(2, effects.takeDamage.size)
				}

				val forceMove = effects.forceMove
				if (effects.removedEffects.contains(sleep)) {
					removeSleepCounter += 1
					if (forceMove != null) assertNotSame(sleep, forceMove.effect)
				}
				if (effects.removedEffects.contains(paralysis)) {
					removeParalysisCounter += 1
					if (forceMove != null) assertNotSame(paralysis, forceMove.effect)
				}

				if (effects.removedEffects.contains(sleep) && effects.removedEffects.contains(paralysis)) {
					assertNull(forceMove)
				}
			}

			// By default, there is a RESISTANCE / 5 chance to remove status effects on the beginning of a turn.
			// Since Animus has 100% resistance against all poison, paralysis, and sleep, that would be 20%.
			assertTrue(removePoisonCounter in 15_000 .. 25_000, "Expected $removePoisonCounter to be 2000")
			assertTrue(removeParalysisCounter in 15_000 .. 25_000, "Expected $removeParalysisCounter to be 2000")

			// Note that sleep already has a base 25% chance to be removed, so it will be 45% in total
			assertTrue(removeSleepCounter in 40_000 .. 50_000, "Expected $removeSleepCounter to be 4500")

			// The chance to remove all 3 effects is 100% * 0.2 * 0.2 * 0.45 = 1.8% chance
			assertTrue(removeAllCounter in 1000 .. 2500, "Expected $removeAllCounter to be 1800")

			// The chance to remove 2 effects is 100% * (0.2 * 0.2 + 0.2 * 0.45 + 0.2 * 0.45) - 1.8% = 20.2% chance
			assertTrue(removeTwoCounter in 15_000 .. 25_000, "Expected $removeTwoCounter to be 20200")
		}
	}
}
