package mardek.importer.skills

import mardek.content.Content
import mardek.importer.util.parseActionScriptResource

fun importSkillsContent(content: Content) {
	val skillsCode = parseActionScriptResource("mardek/importer/stats/skills.txt")

	val skillClasses = parseSkillClasses(
		content, skillsCode.variableAssignments["techInfo"]!!,
		skillsCode.variableAssignments["MONSTER_SKILLS"]!!,
		skillsCode.variableAssignments["TechSpriteMappings"]!!
	)
	val sirenSongs = parseSirenSongs(skillsCode.variableAssignments["SIREN_SONGS"]!!)
	content.skills.classes.addAll(skillClasses)
	content.skills.sirenSongs.addAll(sirenSongs)
	parseReactionSkillsAndPassiveSkills(
		content, rawSkills = skillsCode.variableAssignments["reactionInfo"]!!
	)
}

class SkillParseException(message: String): RuntimeException(message)
