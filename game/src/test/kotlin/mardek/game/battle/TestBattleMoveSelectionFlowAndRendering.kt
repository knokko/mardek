package mardek.game.battle

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionBattle
import mardek.state.ingame.battle.*
import org.junit.jupiter.api.Assertions.*
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

fun testBattleMoveSelectionFlowAndRendering(instance: TestingInstance) {
	instance.apply {
		val campaign = simpleCampaignState()
		val mardekState = campaign.characterStates[heroMardek]!!
		val deuganState = campaign.characterStates[heroDeugan]!!
		mardekState.currentHealth = 20
		deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats, deuganState.activeStatusEffects)
		mardekState.currentMana = mardekState.determineMaxMana(heroMardek.baseStats, deuganState.activeStatusEffects)
		deuganState.currentMana = 20
		mardekState.gainExperience(500)
		startSimpleBattle(campaign)

		val battle = ((campaign.state as AreaState).suspension as AreaSuspensionBattle).battle

		val backgroundColors = arrayOf(
			Color(198, 4, 0), // one of the lava colors
			Color(0, 0, 16), // dark lava color
		)

		val barColors = arrayOf(
			Color(58, 108, 25), // full health bar color
			Color(127, 231, 56), // full health bar text color
			Color(131, 94, 32), // half health bar color
			Color(207, 230, 56), // half health bar text color
			Color(38, 109, 129), // mana bar color
			Color(34, 247, 255), // mana bar text color
			Color(168, 130, 57), // xp bar color
			Color(241, 216, 95), // xp bar text color
			Color(59, 42, 28), // bar background color
		)

		val monsterColors = arrayOf(
			Color(85, 56, 133), // skin color of monster
			Color(74, 49, 117), // 'back' skin color of monster
			Color(255, 255, 204), // teeth color of monster
		)

		val mardekColors = arrayOf(
			Color(129, 129, 79), // pants color of battle model of Mardek
		)

		val deuganColors = arrayOf(
			Color(195, 157, 79), // hair color of battle model of Deugan
		)

		val turnOrderColors = arrayOf(
			Color(133, 96, 53), // one of the turn order monster icon colors
		)

		val pointerColors = arrayOf(
			Color(51, 153, 204),
			Color(0, 50, 153),
			Color(50, 50, 203),
		)

		val targetingColors = arrayOf(
			Color(180, 154, 110),
			Color(175, 61, 1),
			Color(126, 1, 1),
		)

		val elixirColors = arrayOf(
			Color(155, 90, 0),
			Color(182, 141, 0),
			Color(255, 255, 192)
		)

		val powersColors = arrayOf(
			Color(157, 195, 243),
		)

		val onTurnColors = arrayOf(Color(180, 145, 57))

		fun assertSelectedMove(expected: BattleMoveSelection) {
			assertInstanceOf(BattleStateMachine.SelectMove::class.java, battle.state)
			assertEquals(expected, (battle.state as BattleStateMachine.SelectMove).selectedMove)
		}

		val state = InGameState(campaign, "test")

		val shallowColors = backgroundColors + barColors + monsterColors + mardekColors +
				deuganColors + turnOrderColors + pointerColors + onTurnColors
		val context = createUpdateContext(10.milliseconds)
		val sounds = content.audio.fixedEffects

		// Skip waiting & fade-in
		repeat(100) {
			state.update(context)
		}
		assertSelectedMove(BattleMoveSelectionAttack(target = null))
		assertSame(sounds.ui.scroll2, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack0",
			shallowColors, powersColors
		)

		// 'Scroll' to skill selection
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = null, target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill0",
			shallowColors, emptyArray()
		)

		// 'Scroll' to item selection
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = null, target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item0",
			shallowColors, emptyArray()
		)

		// 'Scroll' to wait
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionWait)
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-wait",
			shallowColors, emptyArray()
		)

		// 'Scroll' to flee
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionFlee)
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-flee",
			shallowColors, emptyArray()
		)

		// 'Scroll' to attack
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// 'Dive' into attack target selection
		context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingOpponents()[0]))
		assertSame(sounds.ui.clickConfirm, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack1",
			backgroundColors + pointerColors + mardekColors + deuganColors + targetingColors,
			emptyArray(),
		)

		// 'Scrolling' left has no effect since basic attacks are single-target
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingOpponents()[0]))
		assertNull(context.soundQueue.take())

		// 'Scrolling' right should cause Deugan to become the target
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingPlayers()[1]))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack2",
			backgroundColors + pointerColors + mardekColors + targetingColors,
			emptyArray(),
		)

		// 'Scrolling' right again has no effect since basic attacks are single-target
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingPlayers()[1]))
		assertNull(context.soundQueue.take())

		// 'Cancel' and open item selection
		context.input.postEvent(pressKeyEvent(InputKey.Cancel))
		context.input.postEvent(releaseKeyEvent(InputKey.Cancel))
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
		context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = null))
		assertSame(sounds.ui.clickCancel, context.soundQueue.take())
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertSame(sounds.ui.clickConfirm, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item1",
			backgroundColors + pointerColors + mardekColors + elixirColors, emptyArray()
		)

		// Choose elixir and 'dive into' target selection
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[1]))
		assertSame(sounds.ui.clickConfirm, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// Scrolling right should have no effect because elixirs are single-target
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[1]))
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item2",
			backgroundColors + pointerColors + mardekColors, emptyArray()
		)

		// Scrolling up should cause Mardek to become the target
		context.input.postEvent(pressKeyEvent(InputKey.MoveUp))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[0]))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item3",
			backgroundColors + pointerColors, emptyArray(),
		)

		// Scrolling left twice should only work once since elixirs are single-target
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingOpponents()[0]))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// Cancel item targeting, and go to skill selection
		context.input.postEvent(pressKeyEvent(InputKey.Cancel))
		context.input.postEvent(repeatKeyEvent(InputKey.Cancel))
		context.input.postEvent(releaseKeyEvent(InputKey.Cancel))
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = shock, target = null))
		assertSame(sounds.ui.clickCancel, context.soundQueue.take())
		assertSame(sounds.ui.clickCancel, context.soundQueue.take())
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertSame(sounds.ui.clickConfirm, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill1",
			backgroundColors + pointerColors + powersColors, emptyArray()
		)

		// Scroll to frostasia
		context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
		context.input.postEvent(repeatKeyEvent(InputKey.MoveDown))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill2",
			backgroundColors + pointerColors + powersColors, emptyArray(),
		)

		// Let 'blue targeting blink' wear off
		repeat(100) {
			state.update(context)
		}

		// Choose frostasia and dive into target selection
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
		))
		assertSame(sounds.ui.clickConfirm, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill3",
			backgroundColors + pointerColors + mardekColors + deuganColors, emptyArray()
		)

		// Scrolling left has no effect since there is only 1 enemy
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
		))
		assertNull(context.soundQueue.take())

		// Scroll right once to target Deugan
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingPlayers()[1])
		))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill4",
			backgroundColors + pointerColors + mardekColors, emptyArray()
		)

		// Scroll right again to target both Mardek and Deugan
		context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = BattleSkillTargetAllAllies))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill5",
			backgroundColors + pointerColors, emptyArray()
		)

		// Targeting multiple allies costs too much mana
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = BattleSkillTargetAllAllies))
		assertSame(sounds.ui.clickReject, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// But casting on just Deugan should work...
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		context.input.postEvent(pressKeyEvent(InputKey.Interact))
		context.input.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(BattleStateMachine.CastSkill(
			battle.livingPlayers()[1], arrayOf(battle.livingPlayers()[1]), frostasia,
			null, battleUpdateContext(state.campaign)
		), battle.state)
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())
	}
}

fun testCanNotFlee(instance: TestingInstance) {
	instance.apply {
		val campaign = simpleCampaignState()
		startSimpleBattle(campaign, canFlee = false)

		val battle = ((campaign.state as AreaState).suspension as AreaSuspensionBattle).battle

		fun assertSelectedMove(expected: BattleMoveSelection) {
			assertInstanceOf(BattleStateMachine.SelectMove::class.java, battle.state)
			assertEquals(expected, (battle.state as BattleStateMachine.SelectMove).selectedMove)
		}

		val state = InGameState(campaign, "test")

		val context = createUpdateContext(10.milliseconds)
		val sounds = content.audio.fixedEffects
		// Skip waiting & fade-in
		repeat(100) {
			state.update(context)
		}
		assertSelectedMove(BattleMoveSelectionAttack(target = null))
		assertSame(sounds.ui.scroll2, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// Test that rendering doesn't crash
		testRendering(
			state, 800, 600, "battle-no-flee",
			emptyArray(), emptyArray(),
		)

		// 'Scroll' to skill selection
		context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = null, target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// 'Scroll' to item selection
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = null, target = null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// 'Scroll' to wait
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionWait)
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// Try to scroll to flee, but go to attack, since we cannot flee
		context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(null))
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())

		// Try to scroll to flee from the other direction
		context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionWait)
		assertSame(sounds.ui.scroll1, context.soundQueue.take())
		assertNull(context.soundQueue.take())
	}
}
