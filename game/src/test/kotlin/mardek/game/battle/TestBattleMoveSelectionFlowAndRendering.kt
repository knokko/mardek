package mardek.game.battle

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
import mardek.state.ingame.battle.*
import org.junit.jupiter.api.Assertions.*
import java.awt.Color
import java.lang.Thread.sleep
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
		startSimpleBattle(campaign)

		val battle = campaign.currentArea!!.activeBattle!!

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
			Color(181, 146, 70), // xp bar color
			Color(251, 225, 99), // xp bar text color
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

		fun assertSelectedMove(expected: BattleMoveSelection) {
			assertInstanceOf(BattleStateMachine.SelectMove::class.java, battle.state)
			assertEquals(expected, (battle.state as BattleStateMachine.SelectMove).selectedMove)
		}

		val state = InGameState(campaign, "test")

		val shallowColors = backgroundColors + barColors + monsterColors + mardekColors +
				deuganColors + turnOrderColors + pointerColors
		val fakeInput = InputManager()
		val soundQueue = SoundQueue()
		val context = GameStateUpdateContext(content, fakeInput, soundQueue, 10.milliseconds)
		val sounds = content.audio.fixedEffects
		battle.state = BattleStateMachine.NextTurn(System.nanoTime()) // Skip waiting
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(target = null))
		assertSame(sounds.ui.partyScroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack0",
			shallowColors, powersColors
		)

		// 'Scroll' to skill selection
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = null, target = null))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill0",
			shallowColors, emptyArray()
		)

		// 'Scroll' to item selection
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = null, target = null))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item0",
			shallowColors, emptyArray()
		)

		// 'Scroll' to wait
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionWait)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-wait",
			shallowColors, emptyArray()
		)

		// 'Scroll' to flee
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionFlee)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-flee",
			shallowColors, emptyArray()
		)

		// 'Scroll' to attack
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(target = null))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		// 'Dive' into attack target selection
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingOpponents()[0]))
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack1",
			backgroundColors + pointerColors + mardekColors + deuganColors + targetingColors,
			emptyArray(),
		)

		// 'Scrolling' left has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingOpponents()[0]))
		assertNull(soundQueue.take())

		// 'Scrolling' right should cause Deugan to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingPlayers()[1]))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-attack2",
			backgroundColors + pointerColors + mardekColors + targetingColors,
			emptyArray(),
		)

		// 'Scrolling' right again has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionAttack(battle.livingPlayers()[1]))
		assertNull(soundQueue.take())

		// 'Cancel' and open item selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = null))
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item1",
			backgroundColors + pointerColors + mardekColors + elixirColors, emptyArray()
		)

		// Choose elixir and 'dive into' target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[1]))
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		// Scrolling right should have no effect because elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[1]))
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item2",
			backgroundColors + pointerColors + mardekColors, emptyArray()
		)

		// Scrolling up should cause Mardek to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveUp))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveUp))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingPlayers()[0]))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-item3",
			backgroundColors + pointerColors, emptyArray(),
		)

		// Scrolling left twice should only work once since elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionItem(item = elixir, target = battle.livingOpponents()[0]))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		// Cancel item targeting, and go to skill selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(repeatKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Cancel))
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = shock, target = null))
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill1",
			backgroundColors + pointerColors + powersColors, emptyArray()
		)

		// Scroll to frostasia
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveDown))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = null))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill2",
			backgroundColors + pointerColors + powersColors, emptyArray(),
		)

		// Let 'blue targeting blink' wear off
		sleep(1000)

		// Choose frostasia and dive into target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
		))
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill3",
			backgroundColors + pointerColors + mardekColors + deuganColors, emptyArray()
		)

		// Scrolling left has no effect since there is only 1 enemy
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
		))
		assertNull(soundQueue.take())

		// Scroll right once to target Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(
			skill = frostasia, target = BattleSkillTargetSingle(battle.livingPlayers()[1])
		))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill4",
			backgroundColors + pointerColors + mardekColors, emptyArray()
		)

		// Scroll right again to target both Mardek and Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = BattleSkillTargetAllAllies))
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			state, 800, 600, "battle-select-skill5",
			backgroundColors + pointerColors, emptyArray()
		)

		// Targeting multiple allies costs too much mana
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertSelectedMove(BattleMoveSelectionSkill(skill = frostasia, target = BattleSkillTargetAllAllies))
		assertSame(sounds.ui.clickReject, soundQueue.take())
		assertNull(soundQueue.take())

		// But casting on just Deugan should work...
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(BattleStateMachine.CastSkill(
			battle.livingPlayers()[1], arrayOf(battle.livingPlayers()[1]), frostasia,
			null, battleUpdateContext(state.campaign)
		), battle.state)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())
	}
}
