package mardek.game

import mardek.state.battle.TestBattleState
import mardek.state.battle.TestCombatantReference
import mardek.state.battle.TestMonsterStrategyCalculator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
		TestCombatantReference.testGetStat(instance)
		TestCombatantReference.testGetElementalResistance(instance)
		TestCombatantReference.testGetEffectResistance(instance)
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
}
