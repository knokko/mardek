package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.util.parseActionScriptResource

fun importSkills(combatAssets: CombatAssets, resourcePath: String): SkillAssets {
	val skillsCode = parseActionScriptResource(resourcePath)

	val skillClasses = ArrayList(parseSkillClasses(
		combatAssets, skillsCode.variableAssignments["techInfo"]!!,
		skillsCode.variableAssignments["MONSTER_SKILLS"]!!,
	))
	val sirenSongs = ArrayList(parseSirenSongs(skillsCode.variableAssignments["SIREN_SONGS"]!!))
	val skillAssets = SkillAssets(
		classes = skillClasses, sirenSongs = ArrayList(sirenSongs),
		reactionSkills = ArrayList(), passiveSkills = ArrayList()
	)
	parseReactionSkillsAndPassiveSkills(
		combatAssets, skillAssets, rawSkills = skillsCode.variableAssignments["reactionInfo"]!!
	)
	return skillAssets
}

class SkillParseException(message: String): RuntimeException(message)
