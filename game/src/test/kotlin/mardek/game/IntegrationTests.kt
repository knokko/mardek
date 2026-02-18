package mardek.game

import mardek.game.action.TestActions
import mardek.game.action.TestChatLog
import mardek.game.action.TestLocks
import mardek.game.action.TestMardekHouseActions
import mardek.game.area.TestChestLoot
import mardek.game.area.TestDoors
import mardek.game.area.TestDragonLair
import mardek.game.area.TestRandomBattles
import mardek.game.area.TestTimelines
import mardek.game.area.TestWalking
import mardek.game.battle.*
import mardek.game.inventory.TestCountItemOccurrences
import mardek.game.inventory.TestValidateEquipment
import mardek.game.portrait.TestPortraitImporting
import mardek.game.portrait.TestSpecialPortraitNodes
import mardek.game.save.TestListSaves
import mardek.game.save.TestSaveAndLoad
import mardek.game.story.TestQuests
import mardek.game.ui.TestCutscenes
import mardek.game.ui.TestInGameMenu
import mardek.game.ui.TestInventory
import mardek.game.ui.TestItemStorage
import mardek.game.ui.TestTitleScreen
import mardek.game.ui.TestWorldMap
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
		TestDoors.testDragonLair(instance)
	}

	@Test
	fun testAreaFadeWhenOpeningDoor() {
		TestDoors.testFade(instance)
	}

	@Test
	fun testAreaFadeDuringAreaTransition() {
		TestDoors.testTransitionFade(instance)
	}

	@Test
	fun testDragonLairBossBattle() {
		TestDragonLair.testBossBattle(instance)
	}

	@Test
	fun testHeroesDenWalkTriggerOnlyWorksRightAfterDragonLair() {
		TestTimelines.testHeroesDenTriggerTooLate(instance)
	}

	@Test
	fun testFireTempleDoor() {
		TestDoors.testFireTemple(instance)
	}

	@Test
	fun testBattleMoveSelection() {
		testBattleMoveSelectionFlowAndRendering(instance)
	}

	@Test
	fun testBattleWhereThePlayerCanNotFlee() {
		testCanNotFlee(instance)
	}

	@Test
	fun testPortraits() {
		TestSpecialPortraitNodes.testPresenceAndAbsence(instance)
	}

	@Test
	fun testActions() {
		TestActions.testAreaToArea(instance)
		TestActions.testGlobalActionsToArea(instance)
		TestActions.testSaveCrystalCancel(instance)
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
		TestMonsterStrategyCalculator.testSelectBreathAttack(instance)
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
		TestMoveResultCalculator.testRageChord(instance)
	}

	@Test
	fun testItemResultCalculator() {
		TestMoveResultCalculator.testPotion(instance)
		TestMoveResultCalculator.testEther(instance)
		TestMoveResultCalculator.testElixir(instance)
		TestMoveResultCalculator.testElixirOnSkeleton(instance)
		TestMoveResultCalculator.testPhoenixDown(instance)
		TestMoveResultCalculator.testAntidote(instance)
		TestMoveResultCalculator.testRemedy(instance)
		TestMoveResultCalculator.testAngryJuice(instance)
		TestMoveResultCalculator.testMagicDrink(instance)
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
	fun testChestGold() {
		TestChestLoot.testChestWithGold(instance)
	}

	@Test
	fun testInGameMenu() {
		TestInGameMenu.testOpeningAndScrolling(instance)
	}

	@Test
	fun testDragInventoryEquipment() {
		TestInventory.testMoveEquipment(instance)
	}

	@Test
	fun testEquipmentSlotDefinitions() {
		TestValidateEquipment.testEquipmentSlotDefinitions(instance)
	}

	@Test
	fun testEquipmentStatsHints() {
		TestInventory.testEquipmentStatsHints(instance)
	}

	@Test
	fun testInventoryDiscard() {
		TestInventory.testDiscardItem(instance)
	}

	@Test
	fun testInventorySplitStack() {
		TestInventory.testSplitItemStack(instance)
	}

	@Test
	fun testInventoryConsumeItems() {
		TestInventory.testConsumeItems(instance)
	}

	@Test
	fun testValidateEquipmentDuringInitialization() {
		TestValidateEquipment.testInvalidEquipmentIsErased(instance)
		TestValidateEquipment.testEquipmentInSlotsOfSomeoneElse(instance)
	}

	@Test
	fun testSkillsMenu() {
		TestInGameMenu.testSkills(instance)
	}

	@Test
	fun testNewGameFlow() {
		TestTitleScreen.testNewGame(instance)
	}

	@Test
	fun testChapter1Intro() {
		TestCutscenes.testChapter1Intro(instance)
	}

	@Test
	fun testLoadGameFlow() {
		TestTitleScreen.testLoadGameWithoutCampaignsEdgeCase1(instance)
		TestTitleScreen.testLoadGameWithoutCampaignsEdgeCase2(instance)
		TestTitleScreen.testLoadGameMissingCampaignEdgeCase(instance)
		TestTitleScreen.testLoadGameMissingSaveEdgeCase(instance)
		TestTitleScreen.testLoadGameHappyFlow(instance)
	}

	@Test
	fun testPerformBasicAttack() {
		TestBasicAttacks.testSimpleFlow(instance)
	}

	@Test
	fun testPerformSmiteEvil() {
		TestSkills.testSmiteEvilFlow(instance)
	}

	@Test
	fun testPerformRecover() {
		TestSkills.testRecoverFlow(instance)
	}

	@Test
	fun testPerformFireBreath() {
		TestSkills.testFireBreathFlow(instance)
	}

	@Test
	fun testThrowElixir() {
		TestThrowItems.testElixirFlow(instance)
	}

	@Test
	fun testInfoModal() {
		// TODO CHAP1 Check "I noticed that when selecting commands, the one executed right before the reaction makes the menue select/exit sound."
		TestInfoModal.testRendering(instance)
	}

	@Test
	fun testStatusEffects() {
		TestStatusEffects.testBattleRendering(instance)
		TestStatusEffects.testPoisonAfterWalking(instance)
	}

	@Test
	fun testBeforeTurnStatusEffects() {
		TestTurnStatusEffects.testWithoutStatusEffects(instance)
		TestTurnStatusEffects.testParalysis(instance)
		TestTurnStatusEffects.testSleep(instance)
		TestTurnStatusEffects.testPoisonLowDamage(instance)
		TestTurnStatusEffects.testPoisonHighDamage(instance)
		TestTurnStatusEffects.testRegeneration(instance)
		TestTurnStatusEffects.testMultiple(instance)
		TestTurnStatusEffects.testRemoveEffectsWhenResistanceIsLarge(instance)
	}

	@Test
	fun testTriggerRandomBattles() {
		TestRandomBattles.testNoEncountersBefore30Steps(instance)
		TestRandomBattles.testEncountersAfter30Steps(instance)
		TestRandomBattles.testEncountersAfter60Steps(instance)
		TestRandomBattles.testTransferOddsToNextArea(instance)
		TestRandomBattles.testCannotOpenDoorWhileBattleIsIncoming(instance)
	}

	@Test
	fun testListSaves() {
		TestListSaves.test(instance)
	}

	@Test
	fun testSaveAndLoad() {
		TestSaveAndLoad.testOverwriteVanishedSave(instance)
		TestSaveAndLoad.testHappyFlow(instance)
	}

	@Test
	fun testSaveDuringBasicAttack() {
		TestSaveAndLoad.testSaveDuringBasicAttack(instance)
	}

	@Test
	fun testSaveDuringMagicAttack() {
		TestSaveAndLoad.testSaveDuringMagicAttack(instance)
	}

	@Test
	fun testSaveDuringBattleLoot() {
		TestSaveAndLoad.testSaveDuringBattleLoot(instance)
	}

	@Test
	fun testAreaCharacterPortraitImporting() {
		TestPortraitImporting.testAreaCharacterPortraits(instance)
	}

	@Test
	fun testHeroQuest() {
		TestQuests.testHeroQuest(instance)
	}

	@Test
	fun testSequentialChatLog() {
		TestChatLog.testSequential(instance)
	}

	@Test
	fun testItemStoragePut() {
		TestItemStorage.testPutTunicInStorage(instance)
	}

	@Test
	fun testItemStorageTake() {
		TestItemStorage.testTakeTunicFromStorage(instance)
	}

	@Test
	fun testHeroesHouseWritings() {
		TestActions.testHeroesHouseWritings(instance)
	}

	@Test
	fun testWorldMapBeforeSoothwood() {
		TestWorldMap.testBeforeSoothwood(instance)
	}

	@Test
	fun testSimpleLocks() {
		TestLocks.testFakeDragonLairEntryLock(instance)
		TestLocks.testMonasteryIsNotLockedAtNight(instance)
		TestLocks.testShopsAreLockedAtNight(instance)
		TestLocks.testSewerDoorIsLockedDuringChapter1(instance)
		TestLocks.testWeaponShopIsOpenDuringTheDay(instance)
	}

	@Test
	fun testDropDeuganBeforeFallingStar() {
		TestLocks.testMardekHouseLockToDropDeugan(instance)
	}

	@Test
	fun testGallovarChoiceDialogueChatLog() {
		TestChatLog.testGallovarChoiceChapter1(instance)
	}

	@Test
	fun testGeorgeBlockadeDuringChapter1Night() {
		TestTimelines.testGeorgeBlockadeDuringChapter1Night(instance)
		TestTimelines.testGeorgeIsGoneDuringChapter1Day(instance)
		TestTimelines.testGeorgeBlockadeDuringLastChapter1Night(instance)
		// TODO CHAP2 Test that George is not present during the nights of chapter 2
	}

	@Test
	fun testMotherDialogueAfterDragonLair() {
		TestMardekHouseActions.testMotherDialogueAfterDragonLair(instance)
	}

	@Test
	fun testFallingStarCutsceneAndDialogue() {
		TestMardekHouseActions.testFallingStarCutsceneAndDialogue(instance)
	}

	@Test
	fun testExportingRemovesImageData() {
		ExportingTests.testImageDataWasRemoved(instance)
	}

	@Test
	fun testRandomNpcWalking() {
		TestWalking.testRandomNpcWalking(instance)
	}
}
