package mardek.game.battle

import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.renderer.SharedResources
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestBattleLoot {

	fun testSimpleLoot(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			val monsterFang = content.items.items.find { it.flashName == "Monster Fang" }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = monster, level = 10),
				null, null,
				Enemy(monster = monster, level = 5)
			))

			var numSingleFangs = 0
			var numDoubleFangs = 0
			var totalGold = 0

			val battle = campaign.currentArea!!.activeBattle!!.battle
			repeat(10_000) {
				val loot = generateBattleLoot(content, battle, campaign.getParty())
				totalGold += loot.gold
				assertEquals(0, loot.plotItems.size)
				assertEquals(0, loot.dreamStones.size)
				if (loot.items.size == 1) {
					assertTrue(content.battle.lootItemTexts.contains(
						loot.itemText.replace("You ", "").replace(":", "")
					))
					assertSame(monsterFang, loot.items[0].item)
					if (loot.items[0].amount == 2) {
						numDoubleFangs += 1
					} else {
						assertEquals(1, loot.items[0].amount)
						numSingleFangs += 1
					}
				} else {
					assertEquals(0, loot.items.size)
					assertTrue(content.battle.lootNoItemTexts.contains(loot.itemText))
				}
			}

			// 20% chance to get Monster Fang, and there are 10k fights with 2 monsters each
			// so 4% chance to get 2 fangs and 64% chance to get 0 fangs
			assertTrue(numSingleFangs in 2500 .. 4000, "Expected $numSingleFangs to be 3200")
			assertTrue(numDoubleFangs in 200 .. 600, "Expected $numDoubleFangs to be 400")
			assertTrue(totalGold in 640_000 .. 700_000, "Expected $totalGold to be 670k")
		}
	}

	fun testDoubleGoldTwice(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			// Since Double Gold gives +100% gold, this should triple the total gold,
			// with respect to the previous test case
			val doubleGold = content.skills.passiveSkills.find { it.name == "Double Gold" }!!
			campaign.characterStates[heroMardek]!!.toggledSkills.add(doubleGold)
			campaign.characterStates[heroDeugan]!!.toggledSkills.add(doubleGold)

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = monster, level = 10),
				null, null,
				Enemy(monster = monster, level = 5)
			))

			var totalGold = 0

			val battle = campaign.currentArea!!.activeBattle!!.battle
			repeat(10_000) {
				val loot = generateBattleLoot(content, battle, campaign.getParty())
				totalGold += loot.gold
			}

			assertTrue(
				totalGold in 1950_000 .. 2050_000,
				"Expected $totalGold to be 2million"
			)
		}
	}

	fun testLootFinderTwice(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			// Since Loot Finder Lv.3 gives 5% extra loot chance, this should add 10% in total
			// with respect to the previous test case
			val lootFinder = content.skills.passiveSkills.find { it.name == "Loot Finder Lv.3" }!!
			campaign.characterStates[heroMardek]!!.toggledSkills.add(lootFinder)
			campaign.characterStates[heroDeugan]!!.toggledSkills.add(lootFinder)

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = monster, level = 10),
				null, null,
				Enemy(monster = monster, level = 5)
			))

			var numSingleFangs = 0
			var numDoubleFangs = 0

			val monsterFang = content.items.items.find { it.flashName == "Monster Fang" }!!
			val battle = campaign.currentArea!!.activeBattle!!.battle
			repeat(10_000) {
				val loot = generateBattleLoot(content, battle, campaign.getParty())
				if (loot.items.size == 1) {
					assertSame(monsterFang, loot.items[0].item)
					if (loot.items[0].amount == 2) {
						numDoubleFangs += 1
					} else {
						assertEquals(1, loot.items[0].amount)
						numSingleFangs += 1
					}
				} else assertEquals(0, loot.items.size)
			}

			// 20% base chance to get Monster Fang + 10% from loot finder, and 10k fights with 2 monsters each
			// 9% chance to get 2 monster fangs and 49% chance to get 0 monster fangs
			assertTrue(numDoubleFangs in 600 .. 1200, "Expected $numDoubleFangs to be 900")
			assertTrue(numSingleFangs in 3500 .. 5000, "Expected $numSingleFangs to be 4200")
		}
	}

	fun testPlotLoot(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val demon = content.battle.monsters.find { it.name == "WarportDemon" }!!
			val pass = content.items.plotItems.find { it.name == "Gold Warport Pass" }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = demon, level = 10),
				null, null,
				Enemy(monster = demon, level = 5)
			))

			val battle = campaign.currentArea!!.activeBattle!!.battle
			repeat(100) {
				val loot = generateBattleLoot(content, battle, campaign.getParty())
				assertTrue(content.battle.lootItemTexts.contains(
					loot.itemText.replace("You ", "").replace(":", "")
				))
				assertEquals(1, loot.plotItems.size)
				assertSame(pass, loot.plotItems[0])
			}
		}
	}

	fun testDreamLoot(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val qualna = content.battle.monsters.find { it.name == "Qualna" }!!
			val stone = content.items.dreamstones.find { it.index == 16 }!!
			startSimpleBattle(campaign, enemies = arrayOf(
				Enemy(monster = qualna, level = 10),
				null, null,
				Enemy(monster = qualna, level = 5)
			))

			val battleState = campaign.currentArea!!.activeBattle!!
			repeat(100) {
				val loot = generateBattleLoot(content, battleState.battle, campaign.getParty())
				assertTrue(content.battle.lootItemTexts.contains(
					loot.itemText.replace("You ", "").replace(":", "")
				))
				assertEquals(1, loot.dreamStones.size)
				assertSame(stone, loot.dreamStones[0])
			}
		}
	}

	fun testTakeSingle(instance: TestingInstance) {
		instance.apply {
			val ruby = content.items.items.find { it.flashName == "Ruby" }!!
			val emerald = content.items.items.find { it.flashName == "Emerald" }!!

			val campaign = simpleCampaignState()
			val area = campaign.currentArea!!
			area.battleLoot = BattleLoot(
				123, arrayListOf(ItemStack(ruby, 1), ItemStack(emerald, 2)),
				ArrayList(0), ArrayList(0),
				"bla", listOf(null, heroDeugan, null, null)
			)

			val loot = area.battleLoot!!
			assertEquals(1, loot.selectedPartyIndex)
			assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)

			val soundQueue = SoundQueue()
			val input = InputManager()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)
			campaign.update(context)
			assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)

			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			campaign.update(context)
			assertEquals(BattleLoot.SelectedItem(0), loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			campaign.update(context)
			assertEquals(BattleLoot.SelectedItem(1), loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			// Fill inventory with junk, except slot 10
			val deuganState = campaign.characterStates[heroDeugan]!!
			for (index in deuganState.inventory.indices) {
				deuganState.inventory[index] = ItemStack(Item(), 123)
			}
			deuganState.inventory[10] = null

			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(arrayListOf(ItemStack(ruby, 1)), loot.items)
			assertEquals(BattleLoot.SelectedItem(0), loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.clickConfirm, soundQueue.take())
			assertNull(soundQueue.take())
			assertEquals(ItemStack(emerald, 2), deuganState.inventory[10])

			input.postEvent(repeatKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(arrayListOf(ItemStack(ruby, 1)), loot.items)
			assertEquals(BattleLoot.SelectedItem(0), loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.clickReject, soundQueue.take())
			assertNull(soundQueue.take())
			assertEquals(0, deuganState.countItemOccurrences(ruby))
		}
	}

	fun testTakeAll(instance: TestingInstance) {
		instance.apply {
			val ruby = content.items.items.find { it.flashName == "Ruby" }!!
			val emerald = content.items.items.find { it.flashName == "Emerald" }!!
			val topaz = content.items.items.find { it.flashName == "Topaz" }!!
			val onyx = content.items.items.find { it.flashName == "Onyx" }!!

			val campaign = simpleCampaignState()
			val area = campaign.currentArea!!
			area.battleLoot = BattleLoot(
				123, arrayListOf(
					ItemStack(ruby, 1),
					ItemStack(emerald, 2),
					ItemStack(topaz, 3),
					ItemStack(onyx, 2)
				),
				ArrayList(0), ArrayList(0),
				"bla", listOf(null, heroDeugan, null, heroMardek)
			)

			val loot = area.battleLoot!!
			assertEquals(1, loot.selectedPartyIndex)
			assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)

			val soundQueue = SoundQueue()
			val input = InputManager()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)

			// Fill the inventory of Deugan with junk, except slots 10 and 20
			val deuganState = campaign.characterStates[heroDeugan]!!
			for (index in deuganState.inventory.indices) {
				deuganState.inventory[index] = ItemStack(Item(), 123)
			}
			deuganState.inventory[10] = null
			deuganState.inventory[20] = ItemStack(topaz, 15)

			// Fill the inventory of Mardek with junk, except slots 30 and 40
			val mardekState = campaign.characterStates[heroMardek]!!
			for (index in mardekState.inventory.indices) {
				mardekState.inventory[index] = ItemStack(Item(), 123)
			}
			mardekState.inventory[10] = null
			mardekState.inventory[20] = ItemStack(emerald, 15)

			// Try to give everything to Deugan, although there is not enough space:
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(ItemStack(ruby, 1), deuganState.inventory[10])
			assertEquals(ItemStack(topaz, 18), deuganState.inventory[20])
			assertEquals(arrayListOf(
				ItemStack(emerald, 2), ItemStack(onyx, 2)
			), loot.items)
			assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.clickReject, soundQueue.take())
			assertNull(soundQueue.take())

			// Inventory is full, so retrying won't help
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(ItemStack(ruby, 1), deuganState.inventory[10])
			assertEquals(ItemStack(topaz, 18), deuganState.inventory[20])
			assertEquals(arrayListOf(
				ItemStack(emerald, 2), ItemStack(onyx, 2)
			), loot.items)
			assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.clickReject, soundQueue.take())
			assertNull(soundQueue.take())

			// But Mardek still has just enough space left
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveRight))
			input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(context)
			assertEquals(ItemStack(onyx, 2), mardekState.inventory[10])
			assertEquals(ItemStack(emerald, 17), mardekState.inventory[20])
			assertEquals(0, loot.items.size)
			assertEquals(BattleLoot.SelectedFinish, loot.selectedElement)
			assertSame(content.audio.fixedEffects.ui.scroll, soundQueue.take())
			assertSame(content.audio.fixedEffects.ui.clickConfirm, soundQueue.take())
			assertNull(soundQueue.take())

			// Finish
			input.postEvent(repeatKeyEvent(InputKey.Interact))
			assertSame(loot, area.battleLoot)
			campaign.update(context)
			assertNull(area.battleLoot)
		}
	}

	fun testRendering(instance: TestingInstance) {
		instance.apply {
			val getResources = CompletableFuture<SharedResources>()
			getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))
			val campaign = simpleCampaignState()
			startSimpleBattle(campaign)
			val state = InGameState(campaign)

			val monsterSkinColor = Color(85, 56, 133)
			val area = campaign.currentArea!!

			testRendering(
				getResources, state, 800, 450,
				"loot-before", arrayOf(monsterSkinColor), emptyArray()
			)

			val sapphire = content.items.items.find { it.flashName == "Sapphire" }!!
			area.battleLoot = BattleLoot(
				1234, arrayListOf(ItemStack(sapphire, 5)),
				ArrayList(0), ArrayList(0),
				"Just a test", campaign.characterSelection.party.toList()
			)

			val goldColor = Color(255, 255, 0)
			val goldTextColor = Color(229, 199, 119)
			val goldBackgroundColor = Color(34, 22, 13)
			val mardekSpriteColor = Color(217, 214, 214)
			val deuganSpriteColor = Color(91, 140, 42)
			val partyHighlightColor = Color(99, 128, 177)
			val buttonHighlightColor = Color(152, 190, 222)
			val consumableGridColor = Color(81, 113, 217)
			val buttonTextColor = Color(248, 232, 194)
			val pointerColor = Color(0, 50, 153)
			val buttonBlurredBorderColor = Color(135, 107, 77)
			val sapphireColor = Color(42, 175, 255)
			val gemGridColor = Color(209, 209, 89)
			val baseColors = arrayOf(
				goldColor, goldTextColor, goldBackgroundColor, consumableGridColor, pointerColor,
				mardekSpriteColor, deuganSpriteColor, partyHighlightColor, buttonHighlightColor
			)
			testRendering(
				getResources, state, 800, 450, "loot-initial",
				baseColors + arrayOf(buttonTextColor, sapphireColor),
				arrayOf(monsterSkinColor, gemGridColor)
			)

			val input = InputManager()
			input.postEvent(pressKeyEvent(InputKey.Interact))
			campaign.update(GameStateUpdateContext(content, input, SoundQueue(), 1.seconds))
			testRendering(
				getResources, state, 800, 450, "loot-taken",
				baseColors + arrayOf(buttonBlurredBorderColor, gemGridColor),
				arrayOf(monsterSkinColor, buttonTextColor, sapphireColor)
			)

			getResources.get().destroy()
		}
	}
}
