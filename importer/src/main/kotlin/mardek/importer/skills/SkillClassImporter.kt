package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillClass
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList

fun parseSkillClasses(combatAssets: CombatAssets, rawTechs: String, rawMonsterSkills: String): List<SkillClass> {
	val rawTechsMap = parseActionScriptObject(rawTechs)
	return rawTechsMap.map { entry ->
		val tech = parseActionScriptObject(entry.value)
		var rawSkills = tech["skills"]!!
		if (rawSkills.contains("MONSTER_SKILLS")) rawSkills = rawMonsterSkills
		SkillClass(
			key = entry.key,
			name = parseFlashString(tech["name"]!!, "Tech name")!!,
			description = parseFlashString(tech["desc"]!!, "Tech desc")!!,
			actions = ArrayList(parseActiveSkills(combatAssets, parseActionScriptObjectList(rawSkills)))
		)
	}
}
