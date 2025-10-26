package mardek.importer.inventory

import mardek.content.Content
import mardek.content.inventory.Dreamstone
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.parseActionScriptResource
import java.util.UUID

fun importItemsContent(content: Content) {
	val itemData = parseActionScriptResource("mardek/importer/inventory/data.txt")
	val weaponSheet = sheet("weapons")
	val armorSheet = sheet("armour")
	val miscSheet = sheet("misc")

	importItemTypes(content.items, itemData.variableAssignments["sheetIDs"]!!, itemData.variableAssignments["STACKABLE_TYPES"]!!)
	importWeaponTypes(content, itemData.variableAssignments["wpnIDs"]!!, itemData.variableAssignments["WeaponSFXType"]!!)
	importArmorTypes(content.items, itemData.variableAssignments["ARMOUR_TYPES"]!!)
	importItems(content, itemData.variableAssignments["ItemList"]!!)

	for ((rowIndex, miscType) in arrayOf("accs", "invn", "item", "gems", "plot", "misc", "song").withIndex()) {
		for ((columnIndex, item) in content.items.items.filter { it.type.flashName == miscType }.withIndex()) {
			item.sprite = compressKimSprite3(miscSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}
	for ((columnIndex, item) in content.items.plotItems.withIndex()) {
		item.sprite = compressKimSprite3(miscSheet.getSubimage(16 * columnIndex, 16 * 4, 16, 16))
	}
	for ((rowIndex, armorType) in arrayOf("Sh", "Ar0", "Ar1", "Ar2", "Ar3", "ArR", "ArM", "ArS").withIndex()) {
		for ((columnIndex, item) in content.items.items.filter { it.equipment?.armorType?.key == armorType }.withIndex()) {
			item.sprite = compressKimSprite3(armorSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	for ((columnIndex, item) in content.items.items.filter { it.type.flashName == "helm" }.withIndex()) {
		item.sprite = compressKimSprite3(armorSheet.getSubimage(16 * columnIndex, 16 * 8, 16, 16))
	}

	for ((rowIndex, weaponType) in content.items.weaponTypes.withIndex()) {
		for ((columnIndex, item) in content.items.items.filter { it.equipment?.weapon?.type == weaponType }.withIndex()) {
			item.sprite = compressKimSprite3(weaponSheet.getSubimage(16 * columnIndex, 16 * rowIndex, 16, 16))
		}
	}

	for (index in 1 .. 16) content.items.dreamstones.add(Dreamstone(
		index, UUID.nameUUIDFromBytes("DreamStone$index".encodeToByteArray())
	))
}

class ItemParseException(message: String): RuntimeException(message)
