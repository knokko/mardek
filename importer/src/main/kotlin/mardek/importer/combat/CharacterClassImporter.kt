package mardek.importer.combat

import mardek.assets.combat.CharacterClass
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource

internal fun importClasses(combatAssets: CombatAssets, skillAssets: SkillAssets, inventoryAssets: InventoryAssets) {
	val classCode = parseActionScriptResource("mardek/importer/combat/classes.txt")
	val rawClasses = parseActionScriptObject(classCode.variableAssignments["classStats"]!!)

	for ((key, rawClass) in rawClasses) {
		if (key == "dummy" || key == "soldier") continue
		val classMap = parseActionScriptObject(rawClass)
		val weaponTypeName = parseFlashString(classMap["wpnType"]!!, "character weapon type")!!
		combatAssets.classes.add(CharacterClass(
			rawName = key,
			displayName = parseFlashString(classMap["ClassName"]!!, "character class name")!!,
			skillClass = skillAssets.classes.find {
				it.key == parseFlashString(classMap["tech"]!!, "character tech")!!
			}!!,
			weaponType = if (weaponTypeName == "none") null else inventoryAssets.weaponTypes.find {
				it.flashName == weaponTypeName
			}!!,
			armorTypes = ArrayList(parseActionScriptObject(classMap["amrTypes"]!!).keys.map { typeName ->
				inventoryAssets.armorTypes.find { it.key == typeName }!!
			})
		))
	}
}
