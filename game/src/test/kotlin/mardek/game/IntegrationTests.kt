package mardek.game

import mardek.game.area.TestChestLoot
import mardek.game.area.testDragonLairDoor
import mardek.game.battle.TestBattleLoot
import mardek.game.battle.TestBattleState
import mardek.game.battle.TestCombatantState
import mardek.game.inventory.TestCountItemOccurrences
import mardek.game.battle.TestMonsterStrategyCalculator
import mardek.game.battle.TestMoveResultCalculator
import mardek.game.battle.testBattleMoveSelectionFlowAndRendering
import mardek.game.ui.TestInGameMenu
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
		TestCombatantState.testTransferStatusBack(instance)
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
	fun testBasicAttackResultCalculator() {
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

	@Test
	fun testSkillResultCalculator() {
		TestMoveResultCalculator.testShockDamageAndParalyze(instance)
		TestMoveResultCalculator.testDarkClawBlinding(instance)
		TestMoveResultCalculator.testThousandNeedles(instance)
		TestMoveResultCalculator.testPyromagia(instance)
		TestMoveResultCalculator.testRecover(instance)
		TestMoveResultCalculator.testMultiTargetFrostasia(instance)
	}

	@Test
	fun testBattleLoot() {
		TestBattleLoot.testSimpleLoot(instance)
		TestBattleLoot.testDoubleGoldTwice(instance)
		TestBattleLoot.testLootFinderTwice(instance)
		TestBattleLoot.testPlotLoot(instance)
		TestBattleLoot.testDreamLoot(instance)
		TestBattleLoot.testTakeSingle(instance)
		TestBattleLoot.testTakeAll(instance)
		TestBattleLoot.testRendering(instance)
	}

	@Test
	fun testCountItemOccurrences() {
		TestCountItemOccurrences.testMixed(instance)
	}

	@Test
	fun testChestLoot() {
		TestChestLoot.testControlsAndRendering(instance)
	}

	@Test
	fun testInGameMenu() {
		TestInGameMenu.testOpeningAndScrolling(instance)
	}
}
