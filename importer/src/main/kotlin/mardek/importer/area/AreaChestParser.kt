package mardek.importer.area

import mardek.assets.area.Chest
import mardek.assets.area.ChestBattle
import mardek.assets.area.ChestMonster
import mardek.assets.area.ChestSprite
import mardek.assets.battle.BattleAssets
import mardek.assets.inventory.Dreamstone
import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.ItemStack
import mardek.assets.inventory.PlotItem
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Integer.parseInt

@Suppress("UNCHECKED_CAST")
internal fun parseAreaChests(
	inventoryAssets: InventoryAssets, battleAssets: BattleAssets, rawLoot: String, sprite: ChestSprite
) = parseActionScriptObjectList(rawLoot).map { rawChest ->
	val x = parseInt(rawChest["x"])
	val y = parseInt(rawChest["y"])
	var hidden = false
	val rawHidden = rawChest["hidden"]
	if (rawHidden != null) {
		if (rawHidden == "true" || rawHidden == "1") hidden = true
		else throw AreaParseException("Unexpected chest $rawChest")
	}
	val amount = parseInt(rawChest["amount"])
	val rawItem = parseFlashString(rawChest["item"] ?: rawChest["type"]!!, "chest item")!!

	var stack: ItemStack? = null
	var gold = 0
	var plotItem: PlotItem? = null
	var dreamstone: Dreamstone? = null

	if (rawItem == "gold") {
		gold = amount
	} else if (rawItem == "Dreamstone") {
		dreamstone = inventoryAssets.dreamstones.find { it.index == amount }!!
	} else {
		val item = inventoryAssets.items.find { it.flashName == rawItem }
		if (item == null || rawChest["type"] == "\"plot\"") {
			plotItem = inventoryAssets.plotItems.find { it.name == rawItem }!!
		} else stack = ItemStack(item, amount)
	}

	var battle: ChestBattle? = null
	val rawMonsters = rawChest["monsters"]
	if (rawMonsters != null) {
		val nestedMonsters = parseActionScriptNestedList(rawMonsters)
		if (
			nestedMonsters !is ArrayList<*> || nestedMonsters.size != 4 ||
			nestedMonsters[0] !is ArrayList<*> || nestedMonsters[3] !is String
		) {
			throw AreaParseException("Unexpected chest $rawChest")
		}

		val monsters = Array<ChestMonster?>(4) { null }
		val rawLevels = nestedMonsters[2]
		val levels = if (rawLevels is String) arrayOf(parseInt(rawLevels)) else {
			(rawLevels as ArrayList<String?>).map { if (it != "null") parseInt(it) else null }.toTypedArray()
		}

		val monsterNames2 = if (nestedMonsters[1] == "null") null else nestedMonsters[1] as ArrayList<String>
		for ((index, rawName) in (nestedMonsters[0] as ArrayList<String>).withIndex()) {
			if (rawName == "null") continue
			val name1 = parseFlashString(rawName, "chest monster name 1")!!
			var name2: String? = null
			if (monsterNames2 != null) name2 = parseFlashString(monsterNames2[index], "chest monster name 2")!!

			monsters[index] = ChestMonster(name1, name2, levels[index]!!)
		}

		val layoutName = parseFlashString(nestedMonsters[3] as String, "chest battle position")!!
		val layout = battleAssets.enemyPartyLayouts.find { it.name == layoutName }!!

		val rawSpecialMusic = rawChest["specialMusic"]
		val specialMusic = if (rawSpecialMusic == null) null else parseFlashString(rawSpecialMusic, "chest battle music")!!

		battle = ChestBattle(monsters, layout, specialMusic)
	}

	Chest(
		x = x, y = y, sprite = sprite, hidden = hidden,
		gold = gold, stack = stack, plotItem = plotItem,
		dreamstone = dreamstone, battle = battle
	)
}
