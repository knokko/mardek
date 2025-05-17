package mardek.importer.characters

import mardek.content.Content
import mardek.content.animations.BattleModel
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.StatModifier
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.Skill
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import mardek.content.inventory.ItemStack
import mardek.content.stats.CombatStat
import java.lang.Integer.parseInt

@Suppress("UNCHECKED_CAST")
internal fun importPlayableCharacters(
	content: Content, playerModelMapping: Map<String, BattleModel>?
): List<FatPlayableCharacter> {
	val characters = ArrayList<FatPlayableCharacter>()
	val flashCode = parseActionScriptResource("mardek/importer/stats/playable-characters.txt")

	val rawCharacters = flashCode.variableAssignments["PChatchery"]!!
	for (rawCharacter in parseActionScriptObject(rawCharacters).values) {
		val prefix = "MakeCreatureStats("
		if (!rawCharacter.startsWith(prefix) || !rawCharacter.endsWith(")")) {
			throw CharacterParseException("Unexpected raw playable character $rawCharacter")
		}

		val nestedCharacter = parseActionScriptNestedList("[${rawCharacter.substring(prefix.length, rawCharacter.length - 1)}]") as ArrayList<*>

		val statMods = ArrayList<StatModifier>()
		val statList = nestedCharacter[5] as ArrayList<String>
		statMods.add(StatModifier(CombatStat.Strength, parseInt(statList[0])))
		statMods.add(StatModifier(CombatStat.Vitality, parseInt(statList[1])))
		statMods.add(StatModifier(CombatStat.Spirit, parseInt(statList[2])))
		statMods.add(StatModifier(CombatStat.Agility, parseInt(statList[3])))

		val name = parseFlashString(nestedCharacter[0] as String, "playable character name")!!
		val className = parseFlashString(nestedCharacter[1] as String, "playable character class")!!
		val playable = PlayableCharacter(
			name = name,
			characterClass = content.stats.classes.find { it.rawName == className }!!,
			element = content.stats.elements.find {
				it.rawName == parseFlashString(nestedCharacter[3] as String, "playable character element")!!
			}!!,
			baseStats = statMods,
			areaSprites = content.areas.characterSprites.find {
				it.name == parseFlashString(nestedCharacter[1] as String, "playable character area sprites")
			}!!,
			battleModel = if (playerModelMapping != null) playerModelMapping[className]!! else BattleModel(),
			creatureType = content.stats.creatureTypes.find { it.flashName == "HUMAN" }!!,
		)

		val masteredSkills: MutableList<Skill> = (nestedCharacter[8] as ArrayList<String>).map { rawActiveName ->
			val activeName = parseFlashString(rawActiveName, "playable character action name")!!
			playable.characterClass.skillClass.actions.find { it.name == activeName }!!
		}.toMutableList()

		val toggledSkills = mutableSetOf<Skill>()

		for (otherSkill in nestedCharacter[9] as ArrayList<ArrayList<String>>) {
			if (otherSkill.size != 4) throw CharacterParseException("Unexpected skill $otherSkill")
			val rawSkillType = parseFlashString(otherSkill[0], "character initial skill type")!!
			val skillName = parseFlashString(otherSkill[1], "character initial skill name")
			if (otherSkill[2] != "true" && otherSkill[2] != "0") throw CharacterParseException("Unexpected skill $otherSkill")
			val isMastered = otherSkill[2] == "true"
			if (otherSkill[3] != "true") throw CharacterParseException("Unexpected skill $otherSkill")

			val skill = if (rawSkillType == "PASSIVE") {
				content.skills.passiveSkills.find { it.name == skillName }!!
			} else {
				val skillType = ReactionSkillType.fromString(rawSkillType)
				content.skills.reactionSkills.find { it.type == skillType && it.name == skillName }!!
			}

			if (isMastered) masteredSkills.add(skill)
			toggledSkills.add(skill)
		}

		characters.add(FatPlayableCharacter(
			wrapped = playable,
			initialLevel = parseInt(nestedCharacter[4] as String),
			initialEquipment = (nestedCharacter[6] as ArrayList<String>).map { rawItem ->
				val itemName = parseFlashString(rawItem, "playable character equipment")!!
				if (itemName == "none") null else content.items.items.find { it.flashName == itemName }!!
			},
			initialItems = (nestedCharacter[7] as ArrayList<ArrayList<String>>).map { rawStack ->
				val itemName = parseFlashString(rawStack[0], "playable character item")!!
				val amount = parseInt(rawStack[1])
				ItemStack(content.items.items.find { it.flashName == itemName }!!, amount)
			},
			initialMasteredSkills = masteredSkills,
			initialToggledSkills = toggledSkills,
		))
	}

	for (character in characters) content.playableCharacters.add(character.wrapped)
	return characters
}

internal class CharacterParseException(message: String) : RuntimeException(message)
