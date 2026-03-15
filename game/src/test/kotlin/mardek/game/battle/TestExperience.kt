package mardek.game.battle

import mardek.content.battle.Enemy
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
import mardek.state.ingame.battle.BattleStateMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestExperience {

	fun testKilledByStatusEffectExperience(instance: TestingInstance) {
		instance.apply {
			val area = content.areas.areas.find { it.properties.rawName == "DL_area4" }!!
			val state = InGameState(simpleCampaignState(), "test")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val areaState = AreaState(
				area, state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(6, 20),
			)
			state.campaign.state = areaState

			val mardekState = state.campaign.characterStates[heroMardek]!!
			val deuganState = state.campaign.characterStates[heroDeugan]!!

			val expSkill = content.skills.passiveSkills.find { it.name == "EXP+40%" }!!
			mardekState.currentLevel = 50
			mardekState.toggledSkills.add(expSkill)
			deuganState.currentLevel = 50
			deuganState.toggledSkills.add(expSkill)

			startSimpleBattle(state.campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "mightydragon" }!!,
				level = 40,
			)))

			val battleState = (areaState.suspension as AreaSuspensionBattle).battle
			val monsterState = battleState.livingOpponents()[0]
			monsterState.currentHealth = 1
			monsterState.statusEffects.add(content.stats.statusEffects.find { it.flashName == "BLD" }!!)

			assertEquals(0, mardekState.experienceToNextLevel)
			assertEquals(0, deuganState.experienceToNextLevel)

			// Wait for the bleed damage...
			assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
			sleep(800)
			state.update(updateContext)
			assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
			sleep(1100)
			state.update(updateContext)

			assertEquals(0, monsterState.currentHealth)
			assertInstanceOf<BattleStateMachine.Victory>(battleState.state)

			// These numbers are taken over from vanilla MARDEK
			assertEquals(11200, mardekState.experienceToNextLevel)
			assertEquals(11200, deuganState.experienceToNextLevel)
		}
	}

	// TODO CHAP1 Test level-up & test dead players don't gain XP
	// TODO CHAP1 Test no XP gains when player faints

	fun testExperienceForNextLevel(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val mardekState = state.campaign.characterStates[heroMardek]!!
			assertEquals(1000, mardekState.experienceForNextLevel())

			mardekState.currentLevel = 5
			assertEquals(5000, mardekState.experienceForNextLevel())
		}
	}

	fun testLevelUp(instance: TestingInstance) {
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

			// Give Mardek a weapon that cannot miss
			val mardekState = state.campaign.characterStates[childMardek]!!
			mardekState.equipment[childMardek.characterClass.equipmentSlots[0]] = content.items.items.find {
				it.displayName == "M Blade"
			}!!
			mardekState.gainExperience(899) // Only 101 EXP until level 2

			val deuganState = state.campaign.characterStates[childDeugan]!!

			startSimpleBattle(state.campaign, enemies = arrayOf(null, null, null, Enemy(
				monster = content.battle.monsters.find { it.name == "forest fish" }!!,
				level = 1,
			)))

			val battleState = (areaState.suspension as AreaSuspensionBattle).battle
			val monsterState = battleState.livingOpponents()[0]

			// Deugan is down, so shouldn't get any EXP
			val combatDeugan = battleState.livingPlayers()[1]
			combatDeugan.currentHealth = 0

			assertEquals(899, mardekState.experienceToNextLevel)
			assertEquals(0, deuganState.experienceToNextLevel)

			// Wait until Mardek is on turn
			assertInstanceOf<BattleStateMachine.NextTurn>(battleState.state)
			sleep(800)
			state.update(updateContext)
			assertInstanceOf<BattleStateMachine.SelectMove>(battleState.state)

			// Let Mardek attack the forest fish
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			val moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
			moveToState.finished = true
			state.update(updateContext)

			val strikeState = battleState.state as BattleStateMachine.MeleeAttack.Strike
			strikeState.canDealDamage = true

			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}
			state.update(updateContext)

			// The base EXP of forest fish is 100 * 2, and Mardek should also get 100 EXP for just attacking
			assertEquals(199, mardekState.experienceToNextLevel)
			assertEquals(2, mardekState.currentLevel)
			assertEquals(0, deuganState.experienceToNextLevel)
			assertEquals(1, deuganState.currentLevel)
			assertEquals(0, monsterState.currentHealth)

			testRendering( // TODO CHAP1 Test level-up indicator
				state, 900, 700, "level-up",
				arrayOf(), arrayOf(),
			)
			assertSame(content.audio.effects.find { it.flashName == "hit_MARTIAL" }!!, updateContext.soundQueue.take())
			assertSame(content.audio.fixedEffects.battle.levelUp, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
		}
	}

	fun testPlayerDeath(instance: TestingInstance) {
		instance.apply {

		}
	}
}
