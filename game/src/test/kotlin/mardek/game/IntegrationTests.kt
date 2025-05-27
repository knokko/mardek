package mardek.game

import mardek.state.battle.TestBattleState
import mardek.state.battle.TestCombatantState
import mardek.state.battle.TestMonsterStrategyCalculator
import mardek.state.battle.TestMoveResultCalculator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {

	private lateinit var instance: TestingInstance

	@BeforeAll
	fun importResources() {
		instance = TestingInstance()
	}

	@AfterAll
	fun cleanUp() {
		instance.destroy()
	}

	@Test
	fun testDragonLairDoorAndRendering() {
		testDragonLairDoor(instance)
	}

	@Test
	fun testBattleMoveSelection() {
		testBattleMoveSelectionFlowAndRendering(instance)
	}

	@Test
	fun testBattleState() {
		TestBattleState.testListPlayersAndEnemies(instance)
	}

	@Test
	fun testCombatantReference() {
		TestCombatantState.testGetStat(instance)
		TestCombatantState.testGetElementalResistance(instance)
		TestCombatantState.testGetEffectResistance(instance)
	}

	@Test
	fun testMonsterStrategyCalculator() {
		TestMonsterStrategyCalculator.testMaxUses(instance)
		TestMonsterStrategyCalculator.testMyHp(instance)
		TestMonsterStrategyCalculator.testRepeatAndMyElement(instance)
		TestMonsterStrategyCalculator.testTargetHasEffectAndResistanceAndEvenOdd(instance)
		TestMonsterStrategyCalculator.testTargetMissesEffectAndTargetHp(instance)
		TestMonsterStrategyCalculator.testFaintedAndFreeAllySlots(instance)
		TestMonsterStrategyCalculator.testLowMana(instance)
	}

	@Test
	fun testMoveResultCalculator() {
		TestMoveResultCalculator.testSimpleElementCreatureStunCrit(instance)
		TestMoveResultCalculator.testStrengthAndAttack(instance)
		TestMoveResultCalculator.testDamageReductionAndSoak(instance)
		TestMoveResultCalculator.testComplexElementalModifiers(instance)
		TestMoveResultCalculator.testHealthDrainAgainstUndead(instance)
		TestMoveResultCalculator.testRemoveStatusEffects(instance)
		TestMoveResultCalculator.testAddSleepAfterRemoveSleep(instance)
		TestMoveResultCalculator.testDrainMana(instance)
		TestMoveResultCalculator.testSurvivor(instance)
		TestMoveResultCalculator.testEvasion(instance)
		TestMoveResultCalculator.testShieldAndBerserkDamage(instance)
		TestMoveResultCalculator.testDamageReductionDoesNotHeal(instance)
	}
}
