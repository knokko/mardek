package mardek.game.save

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.BattleStateMachine
import mardek.content.battle.Enemy
import mardek.state.ingame.CampaignState
import mardek.state.saves.SavesFolderManager
import mardek.state.title.TitleScreenState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

object TestSaveAndLoad {

	fun testOverwriteVanishedSave(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val oldSave = createDummySave(saves, "test-save-and-load")

			val areaState = AreaState(dragonLairEntry, AreaPosition(3, 4))
			val state = InGameState(simpleCampaignState(), "test-save-and-load")
			state.campaign.currentArea = areaState
			state.campaign.gold = 21987
			state.campaign.characterStates[heroMardek]!!.currentLevel = 5
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 6

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds, saves)

			// Interact with the crystal, and wait until the first dialogue message is fully rendered
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(10_000) {
				assertSame(state, state.update(context))
			}

			// Move on to the choice between "Save..." and "Cancel..."
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Choose save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			val actions = state.campaign.currentArea!!.actions!!

			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			assertSame(state, state.update(context))
			assertTrue(oldSave.delete())
			assertEquals(1, actions.saveSelectionState!!.selectableFiles.size)

			// Empty the sound queue
			while (true) {
				if (context.soundQueue.take() == null) break
			}

			// Try to overwrite old save, which fails because I deleted it
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickReject, context.soundQueue.take())

			// The system should notice that the file no longer exists
			assertSame(state, state.update(context))
			val saveSelection = actions.saveSelectionState!!
			assertEquals(0, saveSelection.selectableFiles.size)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))
			assertSame(content.audio.fixedEffects.ui.clickConfirm, context.soundQueue.take())
			assertNull(state.campaign.currentArea!!.actions)
		}
	}

	fun testHappyFlow(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val areaState = AreaState(dragonLairEntry, AreaPosition(3, 4))
			var state: GameState = InGameState(simpleCampaignState(), "test-save-and-load")
			(state as InGameState).campaign.run {
				characterStates[heroMardek]!!.currentLevel = 5
				currentArea = areaState
				gold = 21987
			}
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 6

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds, saves)

			// Interact with the crystal, and wait until the first dialogue message is fully rendered
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(10_000) {
				assertSame(state, state.update(context))
			}

			// Move on to the choice between "Save..." and "Cancel..."
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Choose save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			// Create new save
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(context))

			assertNull(state.campaign.currentArea!!.actions)

			// Go to the title screen
			state = TitleScreenState()

			// Click on the "Load Game" button
			state.selectedButton = 1
			context.input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.update(context))
			val saveSelection = state.saveSelection!!
			assertEquals("test-save-and-load", saveSelection.getSelectedCampaign())
			assertEquals(1, saveSelection.selectableFiles.size)

			// Choose the first best save
			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state = state.update(context)
			assertEquals(21987, (state as InGameState).campaign.gold)
			assertEquals(5, state.campaign.characterStates[heroMardek]!!.currentLevel)
			assertEquals(6, state.campaign.characterStates[heroDeugan]!!.currentLevel)
			state.update(context)
			assertNull(state.campaign.currentArea!!.actions)

			val dragonLairColors = arrayOf(
				Color(13, 0, 22), // background color
				Color(77, 69, 95), // brick color
				Color(96, 199, 242), // crystal ring color
				Color(186, 255, 255), // crystal outline color
			)

			testRendering(
				state, 1000, 800, "save-and-load",
				dragonLairColors, emptyArray(),
			)
		}
	}

	fun testSaveDuringBasicAttack(instance: TestingInstance) {
		instance.apply {
			val saves = SavesFolderManager(Files.createTempDirectory("").toFile())
			saves.root.deleteOnExit()

			var campaign = simpleCampaignState()
			var state: GameState = InGameState(campaign, "TestSaveBasicAttack")
			campaign.characterStates[heroMardek]!!.currentLevel = 2
			campaign.characterStates[heroDeugan]!!.currentLevel = 3

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(Enemy(monster, 1), Enemy(monster, 1), null, null))

			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds, saves)
			state.update(updateContext)

			// Do a basic attack on the first monster
			var battle = campaign.currentArea!!.activeBattle!!

			sleep(750)
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertTrue(battle.state is BattleStateMachine.MeleeAttack.MoveTo)

			updateContext.input.postEvent(pressKeyEvent(InputKey.CheatSave))
			state.update(updateContext)

			state = TitleScreenState()
			state.selectedButton = 1

			val beforeLoadTime = System.nanoTime()
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state = state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state = state.update(updateContext)

			campaign = (state as InGameState).campaign
			battle = campaign.currentArea!!.activeBattle!!

			val moveTo = battle.state as BattleStateMachine.MeleeAttack.MoveTo
			assertFalse(moveTo.finished)
			assertSame(battle.livingPlayers()[1], moveTo.attacker)
			assertSame(battle.livingOpponents()[0], moveTo.target)
			assertNull(moveTo.skill)
			moveTo.finished = true
			assertTrue(moveTo.startTime > beforeLoadTime)

			state.update(updateContext)
			val strike = battle.state as BattleStateMachine.MeleeAttack.Strike
			assertFalse(strike.finished)
			assertSame(battle.livingPlayers()[1], strike.attacker)

			strike.hasDealtDamage = true
			strike.finished = true
			state.update(updateContext)

			val jumpBack = battle.state as BattleStateMachine.MeleeAttack.JumpBack
			jumpBack.finished = true

			state.update(updateContext)
			assertTrue(battle.state is BattleStateMachine.NextTurn)
			sleep(1000)
			state.update(updateContext)
			val selectMove = battle.state as BattleStateMachine.SelectMove
			assertSame(battle.livingPlayers()[0], selectMove.onTurn)

			saves.root.deleteRecursively()
		}
	}

	fun testSaveDuringMagicAttack(instance: TestingInstance) {
		instance.apply {
			val saves = SavesFolderManager(Files.createTempDirectory("").toFile())
			saves.root.deleteOnExit()

			var campaign = simpleCampaignState()
			var state: GameState = InGameState(campaign, "TestSaveMagicAttack")
			campaign.characterStates[heroMardek]!!.currentLevel = 2
			campaign.characterStates[heroDeugan]!!.currentLevel = 20
			campaign.healParty()

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(Enemy(monster, 1), Enemy(monster, 1), null, null))

			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds, saves)
			state.update(updateContext)

			// Ues Frostasia on the first monster
			var battle = campaign.currentArea!!.activeBattle!!

			sleep(750)
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			println(battle.state)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			println(battle.state)
			repeat(2) {
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				state.update(updateContext)
				println(battle.state)
			}
			repeat(2) {
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				state.update(updateContext)
				println(battle.state)
			}

			assertInstanceOf<BattleStateMachine.CastSkill>(battle.state)

			updateContext.input.postEvent(pressKeyEvent(InputKey.CheatSave))
			state.update(updateContext)

			state = TitleScreenState()
			state.selectedButton = 1

			val beforeLoadTime = System.nanoTime()
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state = state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state = state.update(updateContext)

			campaign = (state as InGameState).campaign
			battle = campaign.currentArea!!.activeBattle!!

			val castSkill = battle.state as BattleStateMachine.CastSkill
			assertFalse(castSkill.hasFinishedCastingAnimation)
			assertNull(castSkill.calculatedDamage)
			assertSame(battle.livingPlayers()[1], castSkill.caster)
			assertEquals(1, castSkill.targets.size)
			assertSame(battle.livingOpponents()[0], castSkill.targets[0])
			assertSame(frostasia, castSkill.skill)
			castSkill.hasFinishedCastingAnimation = true
			castSkill.calculatedDamage = arrayOf(null)
			assertTrue(castSkill.startTime > beforeLoadTime)

			state.update(updateContext)
			assertInstanceOf<BattleStateMachine.NextTurn>(battle.state)
			sleep(750)
			state.update(updateContext)
			val selectMove = battle.state as BattleStateMachine.SelectMove
			assertSame(battle.livingPlayers()[0], selectMove.onTurn)

			saves.root.deleteRecursively()
		}
	}

	fun testSaveDuringBattleLoot(instance: TestingInstance) {
		instance.apply {
			val saves = SavesFolderManager(Files.createTempDirectory("").toFile())
			saves.root.deleteOnExit()

			var campaign = simpleCampaignState()
			campaign.characterStates[heroMardek]!!.currentLevel = 1
			campaign.characterStates[heroDeugan]!!.currentLevel = 1
			var state: GameState = InGameState(campaign, "TestSaveDuringLoot")

			val animus = content.battle.monsters.find { it.name == "kdestralan_mind" }!!
			startSimpleBattle(campaign, enemies = arrayOf(null, null, null, Enemy(animus, 1)))

			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds, saves)
			state.update(updateContext)

			var battle = campaign.currentArea!!.activeBattle!!
			battle.livingOpponents()[0].currentHealth = 0

			state.update(updateContext)
			sleep(750)
			state.update(updateContext)
			assertInstanceOf<BattleStateMachine.Victory>(battle.state)
			sleep(2000)
			state.update(updateContext)

			var loot = campaign.currentArea!!.battleLoot!!
			val sorcerer = content.items.items.find { it.flashName == "Sorcerer's Soul" }!!
			// The loot should be: Sorcerer's Soul, Scarab of Protection, Elixir, PhoenixPinion
			assertEquals(4, loot.items.size)
			assertTrue(loot.items.contains(ItemStack(sorcerer, 1)))
			assertEquals(0, campaign.characterStates[heroMardek]!!.countItemOccurrences(sorcerer))

			// Take Sorcerer's Soul
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertEquals(3, loot.items.size)
			assertFalse(loot.items.contains(ItemStack(sorcerer, 1)))

			assertEquals(1, campaign.characterStates[heroMardek]!!.countItemOccurrences(sorcerer))

			updateContext.input.postEvent(pressKeyEvent(InputKey.CheatSave))
			state.update(updateContext)

			state = TitleScreenState()
			state.selectedButton = 1

			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state = state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state = state.update(updateContext)

			campaign = (state as InGameState).campaign
			battle = campaign.currentArea!!.activeBattle!!
			loot = campaign.currentArea!!.battleLoot!!

			assertInstanceOf<BattleStateMachine.Victory>(battle.state)
			assertEquals(3, loot.items.size)
			assertEquals(1, campaign.characterStates[heroMardek]!!.countItemOccurrences(sorcerer))

			// Take all loot
			repeat(2) {
				updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
				state.update(updateContext)
			}

			assertNull(campaign.currentArea!!.battleLoot)
			assertNull(campaign.currentArea!!.activeBattle)

			saves.root.deleteRecursively()
		}
	}
}
