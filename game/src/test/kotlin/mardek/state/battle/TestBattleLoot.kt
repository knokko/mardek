package mardek.state.battle

import mardek.game.TestingInstance
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.battle.Enemy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

object TestBattleLoot {

	fun testSimpleLoot(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val monster = content.battle.monsters.find { it.name == "monster" }!!
			val monsterFang = content.items.items.find { it.flashName == "Monster Fang" }!!
			startSimpleBattle(state, enemies = arrayOf(
				Enemy(monster = monster, level = 10),
				null, null,
				Enemy(monster = monster, level = 5)
			))

			var numMonsterFangs = 0
			var numDoubleFangs = 0
			var totalGold = 0
			repeat(10_000) {
				val loot = state.campaign.currentArea!!.activeBattle!!.battle.generateLoot()
				totalGold += loot.gold
				assertEquals(0, loot.plotItems.size)
				assertEquals(0, loot.dreamStones.size)
				if (loot.items.size == 1) {
					assertSame(monsterFang, loot.items[0].item)
					assertTrue(loot.items[0].amount <= 2, "Expected ${loot.items[0].amount} <= 2")
					numMonsterFangs += loot.items[0].amount
					if (loot.items[0].amount == 2) numDoubleFangs += 1
				} else assertEquals(0, loot.items.size)

				// TODO compute gold
			}

			// 20% chance to get Monster Fang, and there are 20k monsters
			assertTrue(numMonsterFangs in 3000 .. 5000, "Expected $numMonsterFangs to be 4000")
			assertTrue(numDoubleFangs in 200 .. 600, "Expected $numDoubleFangs to be 400")
			// TODO test totalGold
		}
	}

	fun testPlotLoot(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val demon = content.battle.monsters.find { it.name == "WarportDemon" }!!
			val pass = content.items.plotItems.find { it.name == "Gold Warport Pass" }!!
			startSimpleBattle(state, enemies = arrayOf(
				Enemy(monster = demon, level = 10),
				null, null,
				Enemy(monster = demon, level = 5)
			))

			repeat(100) {
				val loot = state.campaign.currentArea!!.activeBattle!!.battle.generateLoot()
				assertEquals(1, loot.plotItems.size)
				assertSame(pass, loot.plotItems[0])
			}
		}
	}

	fun testDreamLoot(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLair2, AreaPosition(10, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			))

			val qualna = content.battle.monsters.find { it.name == "Qualna" }!!
			val stone = content.items.dreamstones.find { it.index == 16 }!!
			startSimpleBattle(state, enemies = arrayOf(
				Enemy(monster = qualna, level = 10),
				null, null,
				Enemy(monster = qualna, level = 5)
			))

			repeat(100) {
				val loot = state.campaign.currentArea!!.activeBattle!!.battle.generateLoot()
				assertEquals(1, loot.dreamStones.size)
				assertSame(stone, loot.dreamStones[0])
			}
		}
	}
}
