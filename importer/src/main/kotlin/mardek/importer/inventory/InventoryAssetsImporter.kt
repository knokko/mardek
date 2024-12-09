package mardek.importer.inventory

import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.util.parseActionScriptResource

fun importInventoryAssets(combatAssets: CombatAssets, skillAssets: SkillAssets, resourcePath: String): InventoryAssets {
	val itemData = parseActionScriptResource(resourcePath)

	val assets = InventoryAssets()
	val typeSpriteMapping = importItemTypes(
		assets, itemData.variableAssignments["sheetIDs"]!!, itemData.variableAssignments["STACKABLE_TYPES"]!!
	)
	importWeaponTypes(assets, itemData.variableAssignments["wpnIDs"]!!, itemData.variableAssignments["WeaponSFXType"]!!)
	importArmorTypes(assets, itemData.variableAssignments["ARMOUR_TYPES"]!!)
	importItems(combatAssets, skillAssets, assets, itemData.variableAssignments["ItemList"]!!)

	// TODO sprites

	return assets
}

class ItemParseException(message: String): RuntimeException(message)
