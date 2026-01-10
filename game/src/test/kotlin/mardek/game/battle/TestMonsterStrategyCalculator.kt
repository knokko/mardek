package mardek.game.battle

import mardek.content.battle.Enemy
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import mardek.game.TestingInstance
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.*
import org.junit.jupiter.api.Assertions.*

object TestMonsterStrategyCalculator {

	fun testMaxUses(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val zombieShaman = content.battle.monsters.find { it.name == "canoniashaman" }!!
			val darkGift = zombieShaman.actions.find { it.name == "Dark Gift" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = zombieShaman, level = 5
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			val firstMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
			assertEquals(BattleStateMachine.CastSkill(
				caster, arrayOf(battle.livingOpponents()[0]), darkGift, null, context
			), firstMove)

			val secondMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
			assertNotEquals(firstMove, secondMove)
		}
	}

	fun testMyHp(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val animus = content.battle.monsters.find { it.name == "kdestralan_mind" }!!
			val alpha = animus.actions.find { it.name.contains("Alpha") }!!
			val gamma = animus.actions.find { it.name.contains("Gamma") }!!
			val omega = animus.actions.find { it.name.contains("Omega") }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = animus, level = 50
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			var alphaCounter = 0
			var gammaCounter = 0
			var omegaCounter = 0

			fun countSoulStorms() {
				repeat(10_000) {
					val nextMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
					assertTrue(nextMove is BattleStateMachine.CastSkill)
					val skill = (nextMove as BattleStateMachine.CastSkill).skill
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
			val campaign = simpleCampaignState()

			val masterStone = content.battle.monsters.find { it.name == "masterstone" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = masterStone, level = 50
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			val encounteredElements = mutableSetOf<Element>()
			repeat(100) {
				val rawShiftMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
				val shiftMove = rawShiftMove as BattleStateMachine.CastSkill
				assertTrue(shiftMove.skill.changeElement)
				encounteredElements.add(shiftMove.nextElement!!)
				battle.livingOpponents()[0].element = shiftMove.nextElement!!
				assertArrayEquals(arrayOf(battle.livingOpponents()[0]), shiftMove.targets)

				val rawGemsplosion = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
				val gemsplosionMove = rawGemsplosion as BattleStateMachine.CastSkill
				assertFalse(gemsplosionMove.skill.changeElement)
				assertSame(shiftMove.nextElement!!, gemsplosionMove.skill.element)
				assertArrayEquals(battle.livingPlayers().toTypedArray(), gemsplosionMove.targets)
			}

			assertEquals(7, encounteredElements.size)
		}
	}

	fun testTargetHasEffectAndResistanceAndEvenOdd(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val magicShield = content.stats.statusEffects.find { it.niceName == "M.Shield" }!!
			val mardekState = campaign.characterStates[heroMardek]!!
			val firePendant = content.items.items.find { it.displayName == "FirePendant" }!!
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[4]] = firePendant
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[5]] = firePendant

			val animus = content.battle.monsters.find { it.name == "kdestralan_mind" }!!
			val fireVortex = animus.actions.find { it.name == "Energy Vortex: Fire" }!!
			val waterVortex = animus.actions.find { it.name == "Energy Vortex: Water" }!!
			val shieldBreak = animus.actions.find { it.name == "Shield Breaker: Annihilation" }!!

			startSimpleBattle(campaign,  enemies = arrayOf(null, null, null, Enemy(
				monster = animus, level = 50
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			caster.statusEffects.add(magicShield)
			val context = battleUpdateContext(campaign)

			var fireCounter = 0
			var waterCounterMardek = 0
			var waterCounterDeugan = 0
			var shieldBreakCounter = 0

			fun countMoves() {
				var expectEven = true
				repeat(10_000) {
					val nextMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
					assertTrue(nextMove is BattleStateMachine.CastSkill)
					val skill = (nextMove as BattleStateMachine.CastSkill).skill
					if (skill === fireVortex) {
						assertTrue(expectEven)
						// Mardek has a high fire resistance, so Animus shouldn't cast Energy Vortex: Fire on him
						assertArrayEquals(arrayOf(battle.allPlayers()[1]), nextMove.targets)
						fireCounter += 1
					}
					if (skill === waterVortex) {
						assertTrue(expectEven)
						assertEquals(1, nextMove.targets.size)
						if (nextMove.targets[0] === battle.allPlayers()[0]) waterCounterMardek += 1
						else waterCounterDeugan += 1
					}
					if (skill === shieldBreak) {
						assertFalse(expectEven)
						assertArrayEquals(battle.livingPlayers().toTypedArray(), nextMove.targets)
						shieldBreakCounter += 1
					}
					expectEven = !expectEven
					caster.totalSpentTurns += 1
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
			val campaign = simpleCampaignState()

			val meleeShield = content.stats.statusEffects.find { it.niceName == "Shield" }!!
			val magicShield = content.stats.statusEffects.find { it.niceName == "M.Shield" }!!
			val regen = content.stats.statusEffects.find { it.niceName == "Regeneration" }!!

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			val aalia = content.battle.monsters.find { it.name == "aaliaChapter3" }!!
			val massMeleeShield = aalia.actions.find { it.name == "Shield" }!!
			val massMagicShield = aalia.actions.find { it.name == "M. Shield" }!!
			val massRegen = aalia.actions.find { it.name == "Regen" }!!
			val cura = aalia.actions.find { it.name == "Cura" }!!

			startSimpleBattle(campaign, enemies = arrayOf(Enemy(monster = monster, level = 50), null, null, Enemy(
				monster = aalia, level = 30
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			battle.livingOpponents()[0].statusEffects.add(magicShield)
			val caster = battle.livingOpponents()[1] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			val firstMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
			assertSame(massMeleeShield, firstMove.skill)
			assertArrayEquals(battle.livingOpponents().toTypedArray(), firstMove.targets)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(meleeShield)

			battle.livingOpponents()[0].currentHealth /= 2
			val secondMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
			assertSame(cura, secondMove.skill)
			assertArrayEquals(battle.livingOpponents().toTypedArray(), secondMove.targets)
			battle.livingOpponents()[0].currentHealth *= 2

			val thirdMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
			assertSame(massMagicShield, thirdMove.skill)
			assertArrayEquals(battle.livingOpponents().toTypedArray(), thirdMove.targets)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(magicShield)

			val fourthMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
			assertSame(massRegen, fourthMove.skill)
			assertArrayEquals(battle.livingOpponents().toTypedArray(), fourthMove.targets)
			for (enemy in battle.livingOpponents()) enemy.statusEffects.add(regen)

			val lastMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.MeleeAttack
			assertTrue(battle.livingPlayers().contains(lastMove.target), "Unexpected target ${lastMove.target}")
		}
	}

	fun testFaintedAndFreeAllySlots(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val moric = content.battle.monsters.find { it.name == "moric" }!!
			val zombify = moric.actions.find { it.name == "Zombify" }!!
			val animateDead = moric.actions.find { it.name == "Animate Dead" }!!

			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = moric, level = 30
			)))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			battle.livingPlayers()[0].currentHealth = 0
			run {
				val move = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
				assertSame(zombify, move.skill)
				assertArrayEquals(arrayOf(battle.allPlayers()[0]), move.targets)
				battle.allPlayers()[0].currentHealth = 100
			}

			run {
				val move = MonsterStrategyCalculator(battle, caster, context).determineNextMove() as BattleStateMachine.CastSkill
				assertSame(animateDead, move.skill)
				assertArrayEquals(battle.livingOpponents().toTypedArray(), move.targets)
			}

			fun countAnimateDead(): Int {
				var counter = 0
				repeat(10_000) {
					caster.currentMana = 50
					val move = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
					if (move is BattleStateMachine.CastSkill && move.skill === animateDead) counter += 1
				}
				return counter
			}

			run {
				val count = countAnimateDead()
				assertTrue(count in 5000..7000, "Expected $count to be approximately 6000")
			}

			val simpleEnemy = Enemy(monster = content.battle.monsters.find { it.name == "monster" }!!, level = 10)
			battle.opponents[0] = MonsterCombatantState(
				simpleEnemy.monster, simpleEnemy.level, false, null
			)
			run {
				val count = countAnimateDead()
				assertTrue(count in 1000..3000, "Expected $count to be approximately 2000")
			}

			battle.opponents[1] = MonsterCombatantState(
				simpleEnemy.monster, simpleEnemy.level, false, null
			)
			run {
				val count = countAnimateDead()
				assertTrue(count in 100..1000, "Expected $count to be approximately 500")
			}

			battle.opponents[2] = MonsterCombatantState(
				simpleEnemy.monster, simpleEnemy.level, false, null
			)
			repeat(10_000) {
				val nextMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
				if (nextMove is BattleStateMachine.CastSkill) {
					assertNotSame(animateDead, nextMove.skill)
					assertNotSame(zombify, nextMove.skill)
				}
			}
		}
	}

	fun testLowMana(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val bernard = content.battle.monsters.find { it.name == "bernardChapter3" }!!
			val thunderstorm = bernard.actions.find { it.name == "Thunderstorm" }!!
			val immolate = bernard.actions.find { it.name == "Immolate" }!!
			val glaciate = bernard.actions.find { it.name == "Glaciate" }!!
			val heh = bernard.actions.find { it.name == "Heh." }!!

			startSimpleBattle(campaign, enemies = arrayOf(null, null, Enemy(
				monster = bernard, level = 30
			), null))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			var thunderCounter = 0
			var immolateCounter = 0
			var glaciateCounter = 0
			var hehCounter = 0

			fun countMoves() {
				repeat(10_000) {
					val oldMana = caster.currentMana
					val nextMove = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
					val nextSkill = nextMove as BattleStateMachine.CastSkill
					if (nextSkill.skill === thunderstorm) thunderCounter += 1
					if (nextSkill.skill === immolate) immolateCounter += 1
					if (nextSkill.skill === glaciate) glaciateCounter += 1
					if (nextSkill.skill === heh) hehCounter += 1
					assertEquals(oldMana, caster.currentMana + nextSkill.skill.manaCost)
					if (caster.currentMana > 40) caster.currentMana = 100
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

	fun testSelectBreathAttack(instance: TestingInstance) {
		instance.apply {
			val dragon = content.battle.monsters.find { it.name == "mightydragon" }!!
			val campaign = simpleCampaignState()

			startSimpleBattle(campaign, enemies = arrayOf(null, null, Enemy(
				monster = dragon, level = 30
			), null))
			val battle = (campaign.currentArea!!.suspension as AreaSuspensionBattle).battle
			val caster = battle.livingOpponents()[0] as MonsterCombatantState
			val context = battleUpdateContext(campaign)

			var numBreathAttacks = 0
			repeat(10_000) {
				battle.livingOpponents()[0].currentMana = 100
				val move = MonsterStrategyCalculator(battle, caster, context).determineNextMove()
				if (move is BattleStateMachine.BreathAttack) {
					assertSame(battle.livingOpponents()[0], move.attacker)
					assertTrue(move.skill.isBreath)
					assertFalse(move.skill.damage!!.splitDamage)
					assertEquals(battle.allPlayers(), move.targets.toList())
					assertEquals(SkillTargetType.AllEnemies, move.skill.targetType)
					numBreathAttacks += 1
				} else {
					assertTrue(move is BattleStateMachine.MeleeAttack || move is BattleStateMachine.CastSkill)
				}
			}

			assertTrue(
				numBreathAttacks in 2000 .. 4000,
				"Expected $numBreathAttacks to be approximately 3000"
			)
		}
	}
}
