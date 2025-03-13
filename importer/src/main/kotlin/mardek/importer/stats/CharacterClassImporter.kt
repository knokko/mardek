package mardek.importer.stats

import mardek.content.Content
import mardek.content.combat.CharacterClass
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource

internal fun importClasses(content: Content) {
	val classCode = parseActionScriptResource("mardek/importer/stats/classes.txt")
	val rawClasses = parseActionScriptObject(classCode.variableAssignments["classStats"]!!)

	for ((key, rawClass) in rawClasses) {
		if (key == "dummy" || key == "soldier") continue
		val classMap = parseActionScriptObject(rawClass)
		val weaponTypeName = parseFlashString(classMap["wpnType"]!!, "character weapon type")!!
		content.stats.classes.add(CharacterClass(
			rawName = key,
			displayName = parseFlashString(classMap["ClassName"]!!, "character class name")!!,
			skillClass = content.skills.classes.find {
				it.key == parseFlashString(classMap["tech"]!!, "character tech")!!
			}!!,
			weaponType = if (weaponTypeName == "none") null else content.items.weaponTypes.find {
				it.flashName == weaponTypeName
			}!!,
			armorTypes = ArrayList(parseActionScriptObject(classMap["amrTypes"]!!).keys.map { typeName ->
				content.items.armorTypes.find { it.key == typeName }!!
			})
		))
	}
}
