package mardek.state.battle

import mardek.content.stats.Element
import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.*
import org.junit.jupiter.api.Assertions.*

object TestMonsterStrategyCalculator {

	fun testMaxUses(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val zombieShaman = content.battle.monsters.find { it.name == "CanoniaShaman" }!!
			val darkGift = zombieShaman.actions.find { it.name == "Dark Gift" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = zombieShaman, level = 5
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			val firstMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
			assertEquals(BattleMoveSkill(darkGift, BattleSkillTargetSingle(battle.livingOpponents()[0]), null), firstMove)

			val secondMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
			assertNotEquals(firstMove, secondMove)
		}
	}

	fun testMyHp(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val animus = content.battle.monsters.find { it.name == "Kdestralan_Mind" }!!
			val alpha = animus.actions.find { it.name.contains("Alpha") }!!
			val gamma = animus.actions.find { it.name.contains("Gamma") }!!
			val omega = animus.actions.find { it.name.contains("Omega") }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = animus, level = 50
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			var alphaCounter = 0
			var gammaCounter = 0
			var omegaCounter = 0

			fun countSoulStorms() {
				repeat(10_000) {
					val nextMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
					assertTrue(nextMove is BattleMoveSkill)
					val skill = (nextMove as BattleMoveSkill).skill
					if (skill === alpha) alphaCounter += 1
					if (skill === gamma) gammaCounter += 1
					if (skill === omega) omegaCounter += 1
				}
			}

			countSoulStorms()
			assertTrue(alphaCounter > 0)
			assertEquals(0, gammaCounter)
			assertEquals(0, omegaCounter)

			alphaCounter = 0
			battle.livingOpponents()[0].currentHealth /= 2
			countSoulStorms()
			assertEquals(0, alphaCounter)
			assertTrue(gammaCounter > 0)
			assertEquals(0, omegaCounter)

			gammaCounter = 0
			battle.livingOpponents()[0].currentHealth /= 2
			countSoulStorms()
			assertEquals(0, alphaCounter)
			assertEquals(0, gammaCounter)
			assertTrue(omegaCounter > 0)
		}
	}

	fun testRepeatAndMyElement(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val masterStone = content.battle.monsters.find { it.name == "MasterStone" }!!
			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = masterStone, level = 50
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			val encounteredElements = mutableSetOf<Element>()
			repeat(10_000) {
				val rawShiftMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
				val shiftMove = rawShiftMove as BattleMoveSkill
				assertTrue(shiftMove.skill.changeElement)
				encounteredElements.add(shiftMove.nextElement!!)
				battle.livingOpponents()[0].element = shiftMove.nextElement!!
				assertEquals(BattleSkillTargetSingle(battle.livingOpponents()[0]), shiftMove.target)

				val rawGemsplosion = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
				val gemsplosionMove = rawGemsplosion as BattleMoveSkill
				assertFalse(gemsplosionMove.skill.changeElement)
				assertSame(shiftMove.nextElement!!, gemsplosionMove.skill.element)
				assertEquals(BattleSkillTargetAllEnemies, gemsplosionMove.target)
			}

			assertEquals(7, encounteredElements.size)
		}
	}

	fun testTargetHasEffectAndResistanceAndEvenOdd(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val magicShield = content.stats.statusEffects.find { it.niceName == "M.Shield" }!!
			val mardekState = state.campaign.characterStates[heroMardek]!!
			val firePendant = content.items.items.find { it.flashName == "FirePendant" }!!
			mardekState.equipment[4] = firePendant
			mardekState.equipment[5] = firePendant

			val animus = content.battle.monsters.find { it.name == "Kdestralan_Mind" }!!
			val fireVortex = animus.actions.find { it.name == "Energy Vortex: Fire" }!!
			val waterVortex = animus.actions.find { it.name == "Energy Vortex: Water" }!!
			val shieldBreak = animus.actions.find { it.name == "Shield Breaker: Annihilation" }!!

			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = animus, level = 50
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]
			battle.livingOpponents()[0].statusEffects.add(magicShield)

			var fireCounter = 0
			var waterCounterMardek = 0
			var waterCounterDeugan = 0
			var shieldBreakCounter = 0

			fun countMoves() {
				var expectEven = true
				repeat(10_000) {
					val nextMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
					assertTrue(nextMove is BattleMoveSkill)
					val skill = (nextMove as BattleMoveSkill).skill
					if (skill === fireVortex) {
						assertTrue(expectEven)
						// Mardek has a high fire resistance, so Animus shouldn't cast Energy Vortex: Fire on him
						assertEquals(BattleSkillTargetSingle(battle.allPlayers()[1]), nextMove.target)
						fireCounter += 1
					}
					if (skill === waterVortex) {
						assertTrue(expectEven)
						val target = (nextMove.target as BattleSkillTargetSingle)
						val playerTarget = target.target as PlayerCombatantState
						if (playerTarget === battle.allPlayers()[0]) waterCounterMardek += 1
						else waterCounterDeugan += 1
					}
					if (skill === shieldBreak) {
						assertFalse(expectEven)
						assertEquals(BattleSkillTargetAllEnemies, nextMove.target)
						shieldBreakCounter += 1
					}
					expectEven = !expectEven
					(battle.onTurn as MonsterCombatantState).totalSpentTurns += 1
				}
			}

			countMoves()
			assertEquals(0, shieldBreakCounter) // Neither player has a (magic) shield
			assertTrue(fireCounter > 0)
			assertTrue(waterCounterMardek > 0)
			assertTrue(waterCounterDeugan > 0)

			battle.livingPlayers()[0].statusEffects.add(magicShield)
			fireCounter = 0
			waterCounterMardek = 0
			waterCounterDeugan = 0

			countMoves()
			assertTrue(shieldBreakCounter > 0)
			assertTrue(fireCounter > 0)
			assertTrue(waterCounterMardek > 0)
			assertTrue(waterCounterDeugan > 0)

			shieldBreakCounter = 0
			waterCounterMardek = 0
			battle.livingPlayers()[0].currentHealth = 0
			countMoves()

			assertEquals(0, shieldBreakCounter) // The only player with a magic shield, is down
			assertTrue(fireCounter > 0)
			assertEquals(waterCounterMardek, 0) // Mardek is down
			assertTrue(waterCounterDeugan > 0)
		}
	}

	fun testTargetMissesEffectAndTargetHp(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val meleeShield = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val magicShield = content.stats.statusEffects.find { it.niceName == "M.Shield" }!!
			val regen = content.stats.statusEffects.find { it.niceName == "Regen" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			val aalia = content.battle.monsters.find { it.name == "AaliaChapter3" }!!
			val massMeleeShield = aalia.actions.find { it.name == "Shield" }!!
			val massMagicShield = aalia.actions.find { it.name == "M. Shield" }!!
			val massRegen = aalia.actions.find { it.name == "Regen" }!!
			val cura = aalia.actions.find { it.name == "Cura" }!!

			startSimpleBattle(state, enemies = arrayOf(Enemy(monster = monster, level = 50), null, null, Enemy(
				monster = aalia, level = 30
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.livingOpponents()[0].statusEffects.add(magicShield)
			battle.onTurn = battle.livingOpponents()[1]

			val firstMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
			assertSame(massMeleeShield, firstMove.skill)
			assertEquals(BattleSkillTargetAllAllies, firstMove.target)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(meleeShield)

			battle.livingOpponents()[0].currentHealth /= 2
			val secondMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
			assertSame(cura, secondMove.skill)
			assertEquals(BattleSkillTargetAllAllies, secondMove.target)
			battle.livingOpponents()[0].currentHealth *= 2

			val thirdMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
			assertSame(massMagicShield, thirdMove.skill)
			assertEquals(BattleSkillTargetAllAllies, thirdMove.target)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(magicShield)

			val fourthMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
			assertSame(massRegen, fourthMove.skill)
			assertEquals(BattleSkillTargetAllAllies, fourthMove.target)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(regen)

			val lastMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveBasicAttack
			assertTrue(battle.livingPlayers().contains(lastMove.target), "Unexpected target ${lastMove.target}")
		}
	}

	fun testFaintedAndFreeAllySlots(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val moric = content.battle.monsters.find { it.name == "Moric" }!!
			val zombify = moric.actions.find { it.name == "Zombify" }!!
			val animateDead = moric.actions.find { it.name == "Animate Dead" }!!

			startSimpleBattle(state, enemies = arrayOf(null, null, null, Enemy(
				monster = moric, level = 30
			)))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			battle.livingPlayers()[0].currentHealth = 0
			run {
				val move = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
				assertSame(zombify, move.skill)
				assertEquals(BattleSkillTargetSingle(battle.allPlayers()[0]), move.target)
				battle.allPlayers()[0].currentHealth = 100
			}

			run {
				val move = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove() as BattleMoveSkill
				assertSame(animateDead, move.skill)
				assertEquals(BattleSkillTargetAllAllies, move.target)
			}

			fun countAnimateDead(): Int {
				var counter = 0
				repeat(10_000) {
					val move = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
					if (move is BattleMoveSkill && move.skill === animateDead) counter += 1
				}
				return counter
			}

			run {
				val count = countAnimateDead()
				assertTrue(count in 5000..7000, "Expected $count to be approximately 6000")
			}

			val simpleEnemy = Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 10)
			battle.opponents[0] = MonsterCombatantState(simpleEnemy.monster, simpleEnemy.level, false)
			run {
				val count = countAnimateDead()
				assertTrue(count in 1000..3000, "Expected $count to be approximately 2000")
			}

			battle.opponents[1] = MonsterCombatantState(simpleEnemy.monster, simpleEnemy.level, false)
			run {
				val count = countAnimateDead()
				assertTrue(count in 100..1000, "Expected $count to be approximately 500")
			}

			battle.opponents[2] = MonsterCombatantState(simpleEnemy.monster, simpleEnemy.level, false)
			repeat(10_000) {
				val nextMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
				if (nextMove is BattleMoveSkill) {
					assertNotSame(animateDead, nextMove.skill)
					assertNotSame(zombify, nextMove.skill)
				}
			}
		}
	}

	fun testLowMana(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val bernard = content.battle.monsters.find { it.name == "BernardChapter3" }!!
			val thunderstorm = bernard.actions.find { it.name == "Thunderstorm" }!!
			val immolate = bernard.actions.find { it.name == "Immolate" }!!
			val glaciate = bernard.actions.find { it.name == "Glaciate" }!!
			val heh = bernard.actions.find { it.name == "Heh." }!!

			startSimpleBattle(state, enemies = arrayOf(null, null, Enemy(
				monster = bernard, level = 30
			), null))
			val battle = state.campaign.currentArea!!.activeBattle!!
			battle.onTurn = battle.livingOpponents()[0]

			var thunderCounter = 0
			var immolateCounter = 0
			var glaciateCounter = 0
			var hehCounter = 0

			fun countMoves() {
				repeat(10_000) {
					val nextMove = MonsterStrategyCalculator(battle, battleUpdateContext(state.campaign)).determineNextMove()
					val nextSkill = nextMove as BattleMoveSkill
					if (nextSkill.skill === thunderstorm) thunderCounter += 1
					if (nextSkill.skill === immolate) immolateCounter += 1
					if (nextSkill.skill === glaciate) glaciateCounter += 1
					if (nextSkill.skill === heh) hehCounter += 1
				}
			}

			countMoves()

			assertTrue(
				thunderCounter in 2000..4000,
				"Expected thunderstorm to be used 3000 times, but got $thunderCounter"
			)
			assertTrue(
				immolateCounter in 2500..4500,
				"Expected immolate to be used 3500 times, but got $immolateCounter"
			)
			assertTrue(
				glaciateCounter in 2500..4500,
				"Expected glaciate to be used 3500 times, but got $glaciateCounter"
			)
			assertEquals(0, hehCounter)

			// Thunderstorm, immolate, and glaciate cost 6 mana
			battle.livingOpponents()[0].currentMana = 5

			thunderCounter = 0
			immolateCounter = 0
			glaciateCounter = 0

			countMoves()
			assertEquals(0, thunderCounter)
			assertEquals(0, immolateCounter)
			assertEquals(0, glaciateCounter)
			assertEquals(10_000, hehCounter)
		}
	}
}
