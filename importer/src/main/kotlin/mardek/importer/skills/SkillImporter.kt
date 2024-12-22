package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.util.parseActionScriptResource

fun importSkills(combatAssets: CombatAssets): SkillAssets {
	val skillsCode = parseActionScriptResource("mardek/importer/combat/skills.txt")

	val skillClasses = ArrayList(parseSkillClasses(
		combatAssets, skillsCode.variableAssignments["techInfo"]!!,
		skillsCode.variableAssignments["MONSTER_SKILLS"]!!,
		skillsCode.variableAssignments["TechSpriteMappings"]!!
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
