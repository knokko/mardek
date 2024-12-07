package mardek.importer.inventory

import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.util.parseActionScriptResource

fun importInventoryAssets(combatAssets: CombatAssets, skillAssets: SkillAssets): InventoryAssets {
	val itemData = parseActionScriptResource("mardek/importer/items/data.txt")

	val assets = InventoryAssets()
	importItemTypes(assets, itemData.variableAssignments["sheetIDs"]!!, itemData.variableAssignments["STACKABLE_TYPES"]!!)

	return assets
}
