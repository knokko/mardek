package mardek.importer.inventory

import mardek.content.inventory.ArmorType
import mardek.content.inventory.EquipmentSlotType
import mardek.content.inventory.ItemsContent
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject

fun importArmorTypes(assets: ItemsContent, rawArmorTypes: String) {
	for ((key, rawName) in parseActionScriptObject(rawArmorTypes)) {
		assets.armorTypes.add(ArmorType(
			key = key,
			name = parseFlashString(rawName, "armor type name")!!,
			slot = if (key.startsWith("H")) EquipmentSlotType.Head
					else if (key == "Sh") EquipmentSlotType.OffHand else EquipmentSlotType.Body
		))
	}
	assets.armorTypes.add(ArmorType(key = "ArS", name = "Stole", slot = EquipmentSlotType.Body))
	assets.armorTypes.add(ArmorType(key = "ArM", name = "Legion", slot = EquipmentSlotType.Body))
}
