package mardek.state.battle

import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds

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
}
