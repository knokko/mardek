package mardek.importer.stats

import mardek.content.Content
import mardek.content.inventory.EquipmentSlot
import mardek.content.inventory.ItemType
import mardek.content.skill.SkillClass
import mardek.content.stats.CharacterClass
import mardek.importer.area.parseFlashString
import mardek.importer.inventory.FatItemType
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import java.awt.image.BufferedImage
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

	// The next hardcoded classes are only used for the encyclopedia
	val dummySkillClass = SkillClass(
		key = "encyclopedia dummy",
		name = "encyclopedia dummy",
		description = "dummy skill class for the fake character classes used by non-player people in the encyclopedia",
		actions = ArrayList(0),
		icon = compressKimSprite3(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
	)
	content.skills.classes.add(dummySkillClass)
	content.stats.classes.add(CharacterClass(
		rawName = "muriance",
		displayName = "Bandit Chief",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "vennie",
		displayName = "Thief",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "bernard",
		displayName = "Warlock",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "elwyen_child",
		displayName = "Youngling",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "necromancer",
		displayName = "Necromancer",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "enki",
		displayName = "Wanderer",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "mother",
		displayName = "Mother",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "shop",
		displayName = "Shopkeeper",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "mugbert",
		displayName = "Grunt",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "jaques",
		displayName = "Guard Captain",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "king",
		displayName = "King",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "gallovar",
		displayName = "Medium Priest",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "priest",
		displayName = "High Priest",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "fox",
		displayName = "Grand Adventurer",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "officer",
		displayName = "First Officer",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "advisor",
		displayName = "Trusted Advisor",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "priestess",
		displayName = "Priestess",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "clavis",
		displayName = "Equilibriumancer",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
	content.stats.classes.add(CharacterClass(
		rawName = "qualna",
		displayName = "Diviner",
		skillClass = dummySkillClass,
		equipmentSlots = emptyArray(),
	))
}
