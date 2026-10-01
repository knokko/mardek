package mardek.game.battle

import mardek.content.battle.Enemy
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.ingame.battle.ReactionChallenge
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNotNull
import kotlin.collections.set
import kotlin.time.Duration.Companion.milliseconds

object TestReactionSounds {

	private fun setUp(instance: TestingInstance) = instance.run {
		val state = InGameState(simpleCampaignState(), "test")
		val updateContext = createUpdateContext(10.milliseconds)

		startSimpleBattle(state.campaign, enemies = arrayOf(null, null, null, Enemy(
			monster = content.battle.monsters.find { it.name == "forest fish" }!!,
			level = 1,
		)), backgroundName = "darkwood")

		val battleState = ((state.campaign.state as AreaState).suspension as AreaSuspensionBattle).battle

		// Skip waiting & fade-in
		repeat(100) {
			state.update(updateContext)
		}

		val deuganState = state.campaign.characterStates[heroDeugan]!!
		val increaseDamageSkill = content.skills.reactionSkills.find { it.name == "DMG+1" }!!
		deuganState.skillMastery[increaseDamageSkill] = increaseDamageSkill.masteryPoints
		deuganState.toggledSkills.add(increaseDamageSkill)

		// Let Deugan basic-attack Mardek
		updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
		updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
		updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
		updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
		updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(updateContext)

		val moveToState = battleState.state as BattleStateMachine.MeleeAttack.MoveTo
		state.update(updateContext)
		val reactionChallenge = moveToState.reactionChallenge!!

		Triple(state, updateContext, reactionChallenge)
	}

	fun testEarlyFailSound(instance: TestingInstance) {
		instance.apply {
			val (state, updateContext, reactionChallenge) = setUp(instance)

			state.update(updateContext)

			assertTrue(reactionChallenge.isPending(state.campaign.time))
			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertFalse(reactionChallenge.isPending(state.campaign.time))
			assertFalse(reactionChallenge.wasPassed())
			assertSame(content.audio.fixedEffects.ui.clickReject, updateContext.soundQueue.take())

			// Clicking again shouldn't help
			repeat(60) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertNull(updateContext.soundQueue.take())
			assertFalse(reactionChallenge.isPending(state.campaign.time))
			assertFalse(reactionChallenge.wasPassed())
		}
	}

	fun testLateFailSound(instance: TestingInstance) {
		instance.apply {
			val (state, updateContext, reactionChallenge) = setUp(instance)

			assertTrue(reactionChallenge.isPending(state.campaign.time))

			// Wait until it is too late
			repeat(80) {
				state.update(updateContext)
			}

			assertFalse(reactionChallenge.isPending(state.campaign.time))
			assertFalse(reactionChallenge.wasPassed())

			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertFalse(reactionChallenge.isPending(state.campaign.time))
			assertFalse(reactionChallenge.wasPassed())
			assertSame(content.audio.fixedEffects.ui.clickReject, updateContext.soundQueue.take())

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertNull(updateContext.soundQueue.take())
		}
	}

	fun testNoFailSoundUponSuccess(instance: TestingInstance) {
		instance.apply {
			val (state, updateContext, reactionChallenge) = setUp(instance)

			assertTrue(reactionChallenge.isPending(state.campaign.time))

			// Wait until we should click
			repeat(60) {
				state.update(updateContext)
			}

			assertTrue(reactionChallenge.isPending(state.campaign.time))

			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertFalse(reactionChallenge.isPending(state.campaign.time))
			assertNull(updateContext.soundQueue.take())
			assertTrue(reactionChallenge.wasPassed())

			// Clicking again after passing has no effect
			repeat(20) {
				state.update(updateContext)
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertNull(updateContext.soundQueue.take())
			assertTrue(reactionChallenge.wasPassed())
		}
	}

	private fun prepareReactionMasterySound(
		instance: TestingInstance, state: InGameState,
		reactionChallenge: ReactionChallenge, updateContext: GameStateUpdateContext, renderName: String
	) {
		instance.apply {
			val increaseDamageSkill = content.skills.reactionSkills.find { it.name == "DMG+1" }!!

			val deuganState = state.campaign.characterStates[heroDeugan]!!
			deuganState.equipment[heroDeugan.characterClass.equipmentSlots[0]] = content.items.items.find {
				it.equipment?.skills?.contains(increaseDamageSkill) == true
			}!!
			deuganState.toggledSkills.add(increaseDamageSkill)
			deuganState.skillMastery[increaseDamageSkill] = increaseDamageSkill.masteryPoints - 1

			assertTrue(reactionChallenge.isPending(state.campaign.time))

			testRendering(
				state, 800, 500, "reaction-sounds-${renderName}0",
				emptyArray(), emptyArray(),
			)

			// Wait until we should click
			repeat(60) {
				state.update(updateContext)
			}

			testRendering(
				state, 800, 500, "reaction-sounds-${renderName}1",
				emptyArray(), emptyArray(),
			)

			assertTrue(reactionChallenge.isPending(state.campaign.time))

			while (updateContext.soundQueue.take() != null) {
				updateContext.soundQueue.take()
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(updateContext)

			repeat(40) {
				state.update(updateContext)
			}

			testRendering(
				state, 800, 500, "reaction-sounds-${renderName}2",
				emptyArray(), emptyArray(),
			)

			repeat(40) {
				state.update(updateContext)
			}

			testRendering(
				state, 800, 500, "reaction-sounds-${renderName}3",
				emptyArray(), emptyArray(),
			)

			state.update(updateContext)
		}
	}

	fun testReactionMasteryDisabled(instance: TestingInstance) {
		instance.apply {
			val (state, updateContext, reactionChallenge) = setUp(instance)

			prepareReactionMasterySound(instance, state, reactionChallenge, updateContext, "disabled")

			assertNotNull(updateContext.soundQueue.take()) // either "punch" or "miss"
			assertNull(updateContext.soundQueue.take())
		}
	}

	fun testReactionMasteryEnabled(instance: TestingInstance) {
		instance.apply {
			val (state, updateContext, reactionChallenge) = setUp(instance)
			updateContext.settings.audioSettings.playReactionMasteryJingle = true

			prepareReactionMasterySound(instance, state, reactionChallenge, updateContext, "enabled")

			assertSame(content.audio.fixedEffects.battle.masteredSkill, updateContext.soundQueue.take())
			assertNotNull(updateContext.soundQueue.take()) // either "punch" or "miss"
			assertNull(updateContext.soundQueue.take())
		}
	}
}
