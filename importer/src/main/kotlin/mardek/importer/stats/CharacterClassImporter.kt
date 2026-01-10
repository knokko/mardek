package mardek.importer.stats

import mardek.content.Content
import mardek.content.inventory.EquipmentSlot
import mardek.content.inventory.ItemType
import mardek.content.stats.CharacterClass
import mardek.importer.area.parseFlashString
import mardek.importer.inventory.FatItemType
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import java.util.UUID

internal fun importClasses(content: Content, fatItemTypes: List<FatItemType>) {
	val classCode = parseActionScriptResource("mardek/importer/stats/classes.txt")
	val rawClasses = parseActionScriptObject(classCode.variableAssignments["classStats"]!!)

	for ((key, rawClass) in rawClasses) {
		if (key == "dummy" || key == "soldier") continue
		val classMap = parseActionScriptObject(rawClass)
		val weaponTypeName = parseFlashString(classMap["wpnType"]!!, "character weapon type")!!

		fun itemType(flashName: String): ItemType {
			val fatType = fatItemTypes.find { it.flashName == flashName } ?: throw IllegalArgumentException(
				"Can't find fat item type $flashName: options are ${fatItemTypes.map { it.flashName }}"
			)
			val types = content.items.itemTypes
			return types.find { it.displayName == fatType.displayName } ?: throw IllegalArgumentException(
				"Can't find item type ${fatType.displayName}: options are ${types.map { it.displayName }}"
			)
		}

		fun itemType(fat: FatItemType) = itemType(fat.flashName)

		val allowedArmorTypes = parseActionScriptObject(classMap["amrTypes"]!!).keys.map(::itemType)
		val weaponTypes = if (weaponTypeName == "none") emptyArray() else arrayOf(itemType(weaponTypeName))
		val shieldTypes = allowedArmorTypes.filter { it.displayName == "SHIELD" }
		val helmetTypes = allowedArmorTypes.filter { it.displayName.contains("HELM") }
		val chestplateTypes = allowedArmorTypes.filter { it.displayName.contains("ARMOUR") }
		val accessoryTypes = arrayOf(
			itemType(fatItemTypes.find { it.displayName == "ACCESSORY" }!!),
			itemType(fatItemTypes.find { it.displayName == "GEMSTONE" }!!),
		)

		fun slotId(name: String) = UUID.nameUUIDFromBytes("$key$name".encodeToByteArray())
		content.stats.classes.add(CharacterClass(
			rawName = key,
			displayName = parseFlashString(classMap["ClassName"]!!, "character class name")!!,
			skillClass = content.skills.classes.find {
				it.key == parseFlashString(classMap["tech"]!!, "character tech")!!
			}!!,
			equipmentSlots = arrayOf( // TODO CHAP3 Handle special cases like Elwyen, Mereador, and Legion
				EquipmentSlot(
					id = slotId("main"),
					displayName = "Weapon",
					itemTypes = weaponTypes,
					canBeEmpty = false,
				),
				EquipmentSlot(
					id = slotId("off"),
					displayName = if (shieldTypes.isEmpty()) "" else "Shield",
					itemTypes = shieldTypes.toTypedArray(),
					canBeEmpty = true,
				),
				EquipmentSlot(
					id = slotId("helmet"),
					displayName = "Helmet",
					itemTypes = helmetTypes.toTypedArray(),
					canBeEmpty = true,
				),
				EquipmentSlot(
					id = slotId("chest"),
					displayName = "Armour",
					itemTypes = chestplateTypes.toTypedArray(),
					canBeEmpty = true,
				),
				EquipmentSlot(
					id = slotId("accessory1"),
					displayName = "Accessory",
					itemTypes = accessoryTypes,
					canBeEmpty = true,
				),
				EquipmentSlot(
					id = slotId("accessory2"),
					displayName = "Accessory",
					itemTypes = accessoryTypes,
					canBeEmpty = true,
				),
			),
		))
	}
}
