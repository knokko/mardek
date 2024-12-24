package mardek.importer.skills

import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillClass
import mardek.importer.area.parseFlashString
import mardek.importer.characters.FatPlayableCharacter
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import javax.imageio.ImageIO

fun parseSkillClasses(
	combatAssets: CombatAssets, rawTechs: String,
	rawMonsterSkills: String,
	rawTechSpriteMappings: String
): List<SkillClass> {
	val rawTechsMap = parseActionScriptObject(rawTechs)
	val techSpriteMappings = parseActionScriptNestedList(rawTechSpriteMappings) as ArrayList<*>
	val techIconsInput = FatPlayableCharacter::class.java.classLoader.getResourceAsStream(
		"mardek/importer/combat/tech-icons.png"
	)
	val techIcons = ImageIO.read(techIconsInput)
	techIconsInput.close()
	return rawTechsMap.map { entry ->
		val tech = parseActionScriptObject(entry.value)
		val techName = parseFlashString(tech["name"]!!, "Tech name")!!
		var rawSkills = tech["skills"]!!
		if (rawSkills.contains("MONSTER_SKILLS")) rawSkills = rawMonsterSkills
		val imageIndex = techSpriteMappings.indexOf(tech["name"])
		val icon = techIcons.getSubimage(16 * imageIndex, 0, 16, 16)
		SkillClass(
			key = entry.key,
			name = techName,
			description = parseFlashString(tech["desc"]!!, "Tech desc")!!,
			actions = ArrayList(parseActiveSkills(combatAssets, parseActionScriptObjectList(rawSkills))),
			icon = compressKimSprite1(icon)
		)
	}
}
