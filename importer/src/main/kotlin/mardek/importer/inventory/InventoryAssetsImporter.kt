package mardek.importer.inventory

import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.util.compressSprite
import mardek.importer.util.parseActionScriptResource

fun importInventoryAssets(
	combatAssets: CombatAssets, skillAssets: SkillAssets, resourcePath: String
): InventoryAssets {
	val itemData = parseActionScriptResource(resourcePath)
	val weaponSheet = sheet("weapons")
	val armorSheet = sheet("armour")
	val miscSheet = sheet("misc")

	val assets = InventoryAssets()
	importItemTypes(assets, itemData.variableAssignments["sheetIDs"]!!, itemData.variableAssignments["STACKABLE_TYPES"]!!)
	importWeaponTypes(assets, itemData.variableAssignments["wpnIDs"]!!, itemData.variableAssignments["WeaponSFXType"]!!)
	importArmorTypes(assets, itemData.variableAssignments["ARMOUR_TYPES"]!!)
	importItems(combatAssets, skillAssets, assets, itemData.variableAssignments["ItemList"]!!)

	for ((rowIndex, miscType) in arrayOf("accs", "invn", "item", "gems", "plot", "misc", "song").withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.type.flashName == miscType }.withIndex()) {
			item.sprite = compressSprite(miscSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}
	for ((rowIndex, armorType) in arrayOf("Sh", "Ar0", "Ar1", "Ar2", "Ar3", "ArR", "ArM", "ArS").withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.equipment?.armor?.type?.key == armorType }.withIndex()) {
			item.sprite = compressSprite(armorSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	for ((columnIndex, item) in assets.items.filter { it.type.flashName == "helm" }.withIndex()) {
		item.sprite = compressSprite(armorSheet.getSubimage(16 * columnIndex, 16 * 8, 16, 16))
	}

	for ((rowIndex, weaponType) in assets.weaponTypes.withIndex()) {
		for ((columnIndex, item) in assets.items.filter { it.equipment?.weapon?.type == weaponType }.withIndex()) {
			item.sprite = compressSprite(weaponSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	return assets
}

class ItemParseException(message: String): RuntimeException(message)
