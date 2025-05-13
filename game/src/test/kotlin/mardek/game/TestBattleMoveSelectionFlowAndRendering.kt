package mardek.game

import mardek.input.InputKey
import mardek.input.InputManager
import mardek.renderer.SharedResources
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.*
import org.junit.jupiter.api.Assertions.*
import java.awt.Color
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

fun testBattleMoveSelectionFlowAndRendering(instance: TestingInstance) {
	instance.apply {
		val getResources = CompletableFuture<SharedResources>()
		getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))
		val state = InGameState(CampaignState(
			currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		))
		val mardekState = state.campaign.characterStates[heroMardek]!!
		val deuganState = state.campaign.characterStates[heroDeugan]!!
		mardekState.currentHealth = 20
		deuganState.currentHealth = deuganState.determineMaxHealth(heroDeugan.baseStats, deuganState.activeStatusEffects)
		mardekState.currentMana = mardekState.determineMaxMana(heroMardek.baseStats, deuganState.activeStatusEffects)
		deuganState.currentMana = 20
		startSimpleBattle(state)

		val battle = state.campaign.currentArea!!.activeBattle!!

		val backgroundColors = arrayOf(
			Color(198, 4, 0), // one of the lava colors
			Color(0, 0, 16), // dark lava color
		)

		val barColors = arrayOf(
			Color(14, 243, 8), // earth element color
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
			Color(88, 64, 28), // one of the turn order monster icon colors
		)

		val pointerColors = arrayOf(
			Color(50, 153, 203),
			Color(0, 50, 153),
			Color(50, 50, 203),
		)

		val targetingColors = arrayOf(
			Color(180, 154, 109),
			Color(178, 129, 81),
			Color(175, 59, 0),
		)

		val elixirColors = arrayOf(
			Color(155, 90, 0),
			Color(182, 141, 0),
			Color(255, 255, 192)
		)

		val powersColors = arrayOf(
			Color(254, 254, 194),
			Color(11, 195, 243),
		)

		val shallowColors = backgroundColors + barColors + monsterColors + mardekColors +
				deuganColors + turnOrderColors + pointerColors
		val fakeInput = InputManager()
		val soundQueue = SoundQueue()
		val context = GameStateUpdateContext(content, fakeInput, soundQueue, 10.milliseconds)
		val sounds = content.audio.fixedEffects
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(target = null), battle.selectedMove)
		assertSame(sounds.ui.partyScroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-attack0",
			shallowColors, targetingColors + powersColors
		)

		// 'Scroll' to skill selection
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertEquals(BattleMoveSelectionSkill(skill = null, target = null), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill0",
			shallowColors, targetingColors
		)

		// 'Scroll' to item selection
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertEquals(BattleMoveSelectionItem(item = null, target = null), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-item0",
			shallowColors, targetingColors
		)

		// 'Scroll' to wait
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertTrue(battle.selectedMove is BattleMoveSelectionWait)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-wait",
			shallowColors, targetingColors
		)

		// 'Scroll' to flee
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertTrue(battle.selectedMove is BattleMoveSelectionFlee)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-flee",
			shallowColors, targetingColors
		)

		// 'Scroll' to attack
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertTrue(battle.selectedMove is BattleMoveSelectionAttack)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		// 'Dive' into attack target selection
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(battle.livingOpponents()[0]), battle.selectedMove)
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-attack1",
			backgroundColors + pointerColors + mardekColors + deuganColors + targetingColors,
			turnOrderColors
		)

		// 'Scrolling' left has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(battle.livingOpponents()[0]), battle.selectedMove)
		assertNull(soundQueue.take())

		// 'Scrolling' right should cause Deugan to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(battle.livingPlayers()[1]), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-attack2",
			backgroundColors + pointerColors + mardekColors + targetingColors,
			turnOrderColors
		)

		// 'Scrolling' right again has no effect since basic attacks are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(battle.livingPlayers()[1]), battle.selectedMove)
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
		assertEquals(
			BattleMoveSelectionItem(
				item = elixir, target = null
			), battle.selectedMove)
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-item1",
			backgroundColors + pointerColors + mardekColors + elixirColors, turnOrderColors
		)

		// Choose elixir and 'dive into' target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(
			BattleMoveSelectionItem(
				item = elixir, target = battle.livingPlayers()[1]
			), battle.selectedMove)
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		// Scrolling right should have no effect because elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertEquals(
			BattleMoveSelectionItem(
				item = elixir, target = battle.livingPlayers()[1]
			), battle.selectedMove)
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-item2",
			backgroundColors + pointerColors + mardekColors, turnOrderColors + elixirColors
		)

		// Scrolling up should cause Mardek to become the target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveUp))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveUp))
		state.update(context)
		assertEquals(
			BattleMoveSelectionItem(
				item = elixir, target = battle.livingPlayers()[0]
			), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-item3",
			backgroundColors + pointerColors, turnOrderColors + elixirColors
		)

		// Scrolling left twice should only work once since elixirs are single-target
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertEquals(
			BattleMoveSelectionItem(
				item = elixir, target = battle.livingOpponents()[0]
			), battle.selectedMove)
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
		assertEquals(
			BattleMoveSelectionSkill(
				skill = shock, target = null
			), battle.selectedMove)
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.clickCancel, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill1",
			backgroundColors + pointerColors + powersColors,
			turnOrderColors + elixirColors
		)

		// Scroll to frostasia
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(repeatKeyEvent(InputKey.MoveDown))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveDown))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = null
			), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill2",
			backgroundColors + pointerColors + powersColors,
			turnOrderColors + elixirColors
		)

		// Let 'blue targeting blink' wear off
		sleep(1000)

		// Choose frostasia and dive into target selection
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
			), battle.selectedMove)
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill3",
			backgroundColors + pointerColors + mardekColors + deuganColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + arrayOf(powersColors[0])
		)

		// Scrolling left has no effect since there is only 1 enemy
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = BattleSkillTargetSingle(battle.livingOpponents()[0])
			), battle.selectedMove)
		assertNull(soundQueue.take())

		// Scroll right once to target Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = BattleSkillTargetSingle(battle.livingPlayers()[1])
			), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill4",
			backgroundColors + pointerColors + mardekColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + arrayOf(powersColors[0])
		)

		// Scroll right again to target both Mardek and Deugan
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveRight))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveRight))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = BattleSkillTargetAllAllies
			), battle.selectedMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertNull(soundQueue.take())

		testRendering(
			getResources, state, 800, 600, "battle-select-skill5",
			backgroundColors + pointerColors + arrayOf(powersColors[1]),
			turnOrderColors + elixirColors + arrayOf(powersColors[0])
		)

		// Targeting multiple allies costs too much mana
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(
			BattleMoveSelectionSkill(
				skill = frostasia, target = BattleSkillTargetAllAllies
			), battle.selectedMove)
		assertInstanceOf(BattleMoveThinking::class.java, battle.currentMove)
		assertSame(sounds.ui.clickReject, soundQueue.take())
		assertNull(soundQueue.take())

		// But casting on just Deugan should work...
		fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(releaseKeyEvent(InputKey.MoveLeft))
		fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
		fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
		state.update(context)
		assertEquals(BattleMoveSelectionAttack(null), battle.selectedMove)
		assertEquals(BattleMoveSkill(frostasia, BattleSkillTargetSingle(battle.livingPlayers()[1]), null), battle.currentMove)
		assertSame(sounds.ui.scroll, soundQueue.take())
		assertSame(sounds.ui.clickConfirm, soundQueue.take())
		assertNull(soundQueue.take())

		getResources.get().destroy()
	}
}
