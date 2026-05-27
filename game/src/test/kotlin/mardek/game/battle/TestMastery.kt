package mardek.game.battle

import mardek.content.battle.Enemy
import mardek.content.skill.Skill
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.PlayerCombatantState
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestMastery {

	private class TestContext(
		val state: InGameState,
		val updateContext: GameStateUpdateContext,
		val battleState: BattleState,
	)

	private fun setUp(instance: TestingInstance): TestContext {
		instance.apply {
			val area = content.areas.areas.find { it.properties.rawName == "soothwood" }!!
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			val areaState = AreaState(
				area, state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(1, 1),
			)
			state.campaign.state = areaState

			startSimpleBattle(state.campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "forest fish" }!!,
				level = 1,
			)), backgroundName = "darkwood")

			val battleState = (areaState.suspension as AreaSuspensionBattle).battle
			battleState.state = BattleStateMachine.NextTurn(System.nanoTime()) // Skip waiting
			battleState.startTime = System.nanoTime() - 1000_000_000L // Skip fade-in
			state.update(updateContext)

			return TestContext(state, updateContext, battleState)
		}
	}

	fun testReactionSkillsMasteryBasicAttack(instance: TestingInstance) {
		instance.apply {
			setUp(instance).apply {

				val mardekState = state.campaign.characterStates[childMardek]!!
				val increaseDamageSkill = content.skills.reactionSkills.find { it.name == "DMG+1" }!!
				val blockSkill = content.skills.reactionSkills.find { it.name == "Block" }!!
				mardekState.toggledSkills.add(increaseDamageSkill)
				mardekState.toggledSkills.add(blockSkill)

				val deuganState = state.campaign.characterStates[childDeugan]!!
				val insomniaSkill = content.skills.passiveSkills.find { it.name == "Insomnia" }!!
				deuganState.toggledSkills.add(increaseDamageSkill)
				deuganState.toggledSkills.add(blockSkill)
				deuganState.toggledSkills.add(insomniaSkill)

				// Let Mardek basic-attack Deugan
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
				updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
				state.update(updateContext)

				var moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
				moveToState.finished = true
				moveToState.reactionChallenge!!.forciblyPass()
				state.update(updateContext)

				var strikeState = battleState.state as BattleStateMachine.MeleeAttack.Strike
				strikeState.canDealDamage = true
				state.update(updateContext)
				strikeState.finished = true
				state.update(updateContext)

				var jumpBackState = battleState.state as BattleStateMachine.MeleeAttack.JumpBack
				jumpBackState.finished = true
				state.update(updateContext)

				assertEquals(1, mardekState.performance.numMeleeAttacks)
				assertEquals(0, deuganState.performance.numMeleeAttacks)
				// Note that mardekState.performance.damageDealt can be 0 because Mardek might miss
				assertEquals(0, deuganState.performance.damageDealt)
				assertEquals(0, mardekState.performance.damageReceived)
				assertEquals(deuganState.performance.damageReceived, mardekState.performance.damageDealt)

				assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
				sleep(800)
				state.update(updateContext)
				assertInstanceOf<BattleStateMachine.SelectMove>(battleState.state)

				assertEquals(1, mardekState.skillMastery[increaseDamageSkill])
				assertEquals(1, deuganState.skillMastery[blockSkill])
				assertNull(deuganState.skillMastery[insomniaSkill])

				// Let Deugan basic-attack Mardek
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
				updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
				state.update(updateContext)

				moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
				moveToState.finished = true
				state.update(updateContext)

				strikeState = battleState.state as BattleStateMachine.MeleeAttack.Strike
				strikeState.canDealDamage = true
				strikeState.reactionChallenge!!.forciblyFail()
				state.update(updateContext)
				strikeState.finished = true
				state.update(updateContext)

				jumpBackState = battleState.state as BattleStateMachine.MeleeAttack.JumpBack
				jumpBackState.finished = true
				state.update(updateContext)

				// Mastery shouldn't have increased, since we failed the reaction challenge
				assertEquals(1, mardekState.skillMastery[increaseDamageSkill])
				assertNull(mardekState.skillMastery[blockSkill])
				assertNull(deuganState.skillMastery[increaseDamageSkill])
				assertEquals(1, deuganState.skillMastery[blockSkill])
				assertNull(deuganState.skillMastery[insomniaSkill])

				assertEquals(1, mardekState.performance.numMeleeAttacks)
				assertEquals(1, deuganState.performance.numMeleeAttacks)

				// Let the forest fish attack
				assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
				sleep(800)
				state.update(updateContext)

				moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
				moveToState.finished = true
				state.update(updateContext)

				strikeState = battleState.state as BattleStateMachine.MeleeAttack.Strike
				strikeState.canDealDamage = true
				strikeState.reactionChallenge!!.forciblyFail()
				state.update(updateContext)
				strikeState.finished = true
				state.update(updateContext)

				jumpBackState = battleState.state as BattleStateMachine.MeleeAttack.JumpBack
				jumpBackState.finished = true
				state.update(updateContext)

				assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
				sleep(800)
				state.update(updateContext)
				assertInstanceOf<BattleStateMachine.SelectMove>(battleState.state)

				// Finally run away, and check that nothing else was mastered
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				state.update(updateContext)
				assertNull((state.campaign.state as AreaState).suspension)

				assertEquals(1, mardekState.skillMastery[increaseDamageSkill])
				assertNull(mardekState.skillMastery[blockSkill])
				assertNull(deuganState.skillMastery[increaseDamageSkill])
				assertEquals(1, deuganState.skillMastery[blockSkill])
				assertNull(deuganState.skillMastery[insomniaSkill])

				// Running away does NOT count as victory
				assertEquals(0, mardekState.performance.numBattles)
				assertEquals(0, deuganState.performance.numBattles)
			}
		}
	}

	fun testReactionSkillsMasteryStrike(instance: TestingInstance) {
		instance.apply {
			setUp(instance).apply {
				val necklace = content.items.items.find { it.displayName == "Cog Necklace" }!!
				val mardekState = state.campaign.characterStates[childMardek]!!
				mardekState.equipment[childMardek.characterClass.equipmentSlots[5]] = necklace
				val strikeSkill = childMardek.characterClass.skillClass.actions.find { it.name == "Strike" }!!
				val increaseDamageSkill = content.skills.reactionSkills.find { it.name == "DMG+1" }!!
				val insomniaSkill = content.skills.passiveSkills.find { it.name == "Insomnia" }!!
				mardekState.toggledSkills.add(increaseDamageSkill)
				mardekState.skillMastery[increaseDamageSkill] = increaseDamageSkill.masteryPoints
				mardekState.skillMastery[strikeSkill] = strikeSkill.masteryPoints - 1

				// Let Mardek use Strike on the forest fish
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

				state.update(updateContext)

				val masteredColors = arrayOf(
					Color(213, 0, 0), // Outer color
					Color(255, 147, 26), // Inner color
				)
				val notMasteredColors = arrayOf(
					Color(254, 95, 95), // Text color
					Color(126, 0, 0), // Shadow color
				)
				sleep(500)
				testRendering(
					state, 1600, 1400, "strike-before-mastered",
					notMasteredColors, masteredColors,
				)

				val moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
				moveToState.finished = true
				moveToState.reactionChallenge!!.forciblyPass()

				while (updateContext.soundQueue.take() != null) updateContext.soundQueue.take()
				state.update(updateContext)
				val sounds = content.audio.fixedEffects
				assertSame(sounds.battle.masteredSkill, updateContext.soundQueue.take())
				assertNull(updateContext.soundQueue.take())
				testRendering(
					state, 1600, 1400, "strike-mastered",
					masteredColors, notMasteredColors,
				)

				val strikeState = battleState.state as BattleStateMachine.MeleeAttack.Strike
				strikeState.canDealDamage = true
				state.update(updateContext)
				strikeState.finished = true
				state.update(updateContext)

				assertEquals(1, mardekState.performance.numMeleeAttacks)
				assertEquals(0, mardekState.performance.numMagicSkills)

				// Make sure the forest fish dies, regardless of whether Mardek misses
				if (battleState.livingOpponents().isNotEmpty()) {
					battleState.livingOpponents()[0].currentHealth = 0
				}

				val jumpBackState = battleState.state as BattleStateMachine.MeleeAttack.JumpBack
				jumpBackState.finished = true
				state.update(updateContext)

				assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
				sleep(800)
				state.update(updateContext)
				assertInstanceOf<BattleStateMachine.Victory>(battleState.state)

				assertEquals(strikeSkill.masteryPoints, mardekState.skillMastery[strikeSkill])
				assertEquals(increaseDamageSkill.masteryPoints, mardekState.skillMastery[increaseDamageSkill])
				assertNull(mardekState.skillMastery[insomniaSkill])
				assertEquals(
					hashSetOf(strikeSkill),
					(battleState.livingPlayers()[0] as PlayerCombatantState).masteredSkillsThisBattle,
				)
			}
		}
	}

	// TODO CHAP2 testReactionSkillsMasteryRangedSkill

	fun testPassiveSkillsMastery(instance: TestingInstance) {
		instance.apply {
			setUp(instance).apply {
				val necklace = content.items.items.find { it.displayName == "Cog Necklace" }!!
				val mardekState = state.campaign.characterStates[childMardek]!!
				val deuganState = state.campaign.characterStates[childDeugan]!!
				mardekState.equipment[childMardek.characterClass.equipmentSlots[5]] = necklace
				deuganState.equipment[childDeugan.characterClass.equipmentSlots[5]] = necklace
				val insomniaSkill = content.skills.passiveSkills.find { it.name == "Insomnia" }!!
				mardekState.toggledSkills.add(insomniaSkill)
				mardekState.skillMastery[insomniaSkill] = insomniaSkill.masteryPoints - 1
				deuganState.toggledSkills.add(insomniaSkill)

				// Let's keep this battle short
				battleState.livingOpponents()[0].currentHealth = 0

				// Just skip turn, which should cause the battle to end
				updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
				state.update(updateContext)

				assertInstanceOf<BattleStateMachine.Wait>(battleState.state)
				sleep(300)
				state.update(updateContext)
				assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
				sleep(800)

				while (updateContext.soundQueue.take() != null) updateContext.soundQueue.take()
				state.update(updateContext)
				assertSame(content.audio.fixedEffects.battle.masteredSkill, updateContext.soundQueue.take())
				assertInstanceOf<BattleStateMachine.Victory>(battleState.state)

				assertEquals(1, mardekState.performance.numBattles)

				// Sanity check: we should only increase the Insomnia mastery once, no matter how often we update
				repeat(10) {
					state.update(updateContext)
				}
				sleep(3050)
				state.update(updateContext)
				assertNull(updateContext.soundQueue.take())

				// Take all loot items, if there are any
				val suspension = (state.campaign.state as AreaState).suspension as AreaSuspensionBattle
				val loot = suspension.loot!!
				if (loot.items.isNotEmpty()) {
					updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
					updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
					state.update(updateContext)
				}

				// Finish the loot screen, which should take us to the mastery screen
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
				assertFalse(loot.showMasteryScreen)
				assertEquals(hashMapOf(
					Pair(childMardek, hashSetOf(insomniaSkill)),
					Pair(childDeugan, hashSetOf())
				), loot.masteredSkills)

				while (updateContext.soundQueue.take() != null) updateContext.soundQueue.take()
				state.update(updateContext)
				assertSame(content.audio.fixedEffects.ui.clickConfirm, updateContext.soundQueue.take())
				assertSame(
					content.audio.fixedEffects.battle.masteredSkill,
					updateContext.soundQueue.take()
				)
				assertNull(updateContext.soundQueue.take())
				assertTrue(loot.showMasteryScreen)

				val expectedColors = arrayOf(
					Color(255, 204, 153), // Skin color
					Color(129, 129, 79), // Tunic color
					Color(195, 157, 79), // Deugan hair color
					Color(235, 200, 130), // Text color
					Color(73, 118, 67), // Passive skill icon color
					Color(22, 13, 13), // Upper bar color
					Color(128, 79, 34), // Upper bar text color
					Color(109, 93, 81), // "E" button text color
					Color(167, 161, 141), // "E" button light color
					Color(145, 137, 112), // "E" button dark color
				)
				testRendering(
					state, 800, 600, "mastery-screen",
					expectedColors, arrayOf(),
				)

				// Test that this doesn't crash
				dummySaveManager().createSave(
					content, state.campaign,
					"DoNotCrash", SaveFile.Type.Cheat,
				)

				// Close the mastery screen
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				state.update(updateContext)
				sleep(550)
				state.update(updateContext)
				assertNull((state.campaign.state as AreaState).suspension)

				assertEquals(insomniaSkill.masteryPoints, mardekState.skillMastery[insomniaSkill])
				assertEquals(1, deuganState.skillMastery[insomniaSkill])
				assertEquals(
					hashSetOf(insomniaSkill),
					(battleState.livingPlayers()[0] as PlayerCombatantState).masteredSkillsThisBattle
				)
				assertEquals(
					emptySet<Skill>(),
					(battleState.livingPlayers()[1] as PlayerCombatantState).masteredSkillsThisBattle
				)
			}
		}
	}
}
