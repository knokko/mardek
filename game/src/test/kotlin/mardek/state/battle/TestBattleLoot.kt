package mardek.state.battle

import mardek.game.TestingInstance
import mardek.state.ingame.area.loot.generateBattleLoot
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

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

			var numMonsterFangs = 0
			var numDoubleFangs = 0
			var totalGold = 0

			val battle = campaign.currentArea!!.activeBattle!!.battle
			repeat(10_000) {
				val loot = generateBattleLoot(content, battle, campaign.getParty())
				totalGold += loot.gold
				assertEquals(0, loot.plotItems.size)
				assertEquals(0, loot.dreamStones.size)
				if (loot.items.size == 1) {
					assertTrue(content.battle.lootItemTexts.contains(loot.itemText.replace("You ", "")))
					assertSame(monsterFang, loot.items[0].item)
					assertTrue(loot.items[0].amount <= 2, "Expected ${loot.items[0].amount} <= 2")
					numMonsterFangs += loot.items[0].amount
					if (loot.items[0].amount == 2) numDoubleFangs += 1
				} else {
					assertEquals(0, loot.items.size)
					assertTrue(content.battle.lootNoItemTexts.contains(loot.itemText))
				}
			}

			// 20% chance to get Monster Fang, and there are 20k monsters
			assertTrue(numMonsterFangs in 3000 .. 5000, "Expected $numMonsterFangs to be 4000")
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
					assertTrue(loot.items[0].amount <= 2, "Expected ${loot.items[0].amount} <= 2")
					numSingleFangs += loot.items[0].amount
					if (loot.items[0].amount == 2) numDoubleFangs += 1
				} else assertEquals(0, loot.items.size)
			}

			// 20% base chance to get Monster Fang + 10% from loot finder, and 20k monsters
			println("#single is $numSingleFangs and #double is $numDoubleFangs")
			assertTrue(numSingleFangs in 4000 .. 6000, "Expected $numSingleFangs to be 5100")
			assertTrue(numDoubleFangs in 600 .. 1200, "Expected $numDoubleFangs to be 900")
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
				assertTrue(content.battle.lootItemTexts.contains(loot.itemText))
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
				assertTrue(content.battle.lootItemTexts.contains(loot.itemText))
				assertEquals(1, loot.dreamStones.size)
				assertSame(stone, loot.dreamStones[0])
			}
		}
	}
}
