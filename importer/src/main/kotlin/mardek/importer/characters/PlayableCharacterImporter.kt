package mardek.importer.characters

import mardek.assets.area.AreaAssets
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatAssets
import mardek.assets.combat.StatModifier
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.ReactionSkillType
import mardek.assets.skill.Skill
import mardek.assets.skill.SkillAssets
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import mardek.assets.inventory.ItemStack
import java.lang.Integer.parseInt

@Suppress("UNCHECKED_CAST")
internal fun importPlayableCharacters(
	combatAssets: CombatAssets,
	skillAssets: SkillAssets,
	inventoryAssets: InventoryAssets,
	areaAssets: AreaAssets,
): List<FatPlayableCharacter> {
	val characters = ArrayList<FatPlayableCharacter>()
	val flashCode = parseActionScriptResource("mardek/importer/combat/playable-characters.txt")

	val rawCharacters = flashCode.variableAssignments["PChatchery"]!!
	for (rawCharacter in parseActionScriptObject(rawCharacters).values) {
		val prefix = "MakeCreatureStats("
		if (!rawCharacter.startsWith(prefix) || !rawCharacter.endsWith(")")) {
			throw CharacterParseException("Unexpected raw playable character $rawCharacter")
		}

		val nestedCharacter = parseActionScriptNestedList("[${rawCharacter.substring(prefix.length, rawCharacter.length - 1)}]") as ArrayList<*>

		val statMods = ArrayList<StatModifier>()
		val statList = nestedCharacter[5] as ArrayList<String>
		statMods.add(StatModifier(combatAssets.stats.find { it.flashName == "STR" }!!, parseInt(statList[0])))
		statMods.add(StatModifier(combatAssets.stats.find { it.flashName == "VIT" }!!, parseInt(statList[1])))
		statMods.add(StatModifier(combatAssets.stats.find { it.flashName == "SPR" }!!, parseInt(statList[2])))
		statMods.add(StatModifier(combatAssets.stats.find { it.flashName == "AGL" }!!, parseInt(statList[3])))

		val playable = PlayableCharacter(
			name = parseFlashString(nestedCharacter[0] as String, "playable character name")!!,
			characterClass = combatAssets.classes.find {
				it.rawName == parseFlashString(nestedCharacter[1] as String, "playable character class")!!
			}!!,
			element = combatAssets.elements.find {
				it.rawName == parseFlashString(nestedCharacter[3] as String, "playable character element")!!
			}!!,
			baseStats = statMods,
			areaSprites = areaAssets.characterSprites.find {
				it.name == parseFlashString(nestedCharacter[1] as String, "playable character area sprites")
			}!!
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
				skillAssets.passiveSkills.find { it.name == skillName }!!
			} else {
				val skillType = ReactionSkillType.fromString(rawSkillType)
				skillAssets.reactionSkills.find { it.type == skillType && it.name == skillName }!!
			}

			if (isMastered) masteredSkills.add(skill)
			toggledSkills.add(skill)
		}

		characters.add(FatPlayableCharacter(
			wrapped = playable,
			initialLevel = parseInt(nestedCharacter[4] as String),
			initialEquipment = (nestedCharacter[6] as ArrayList<String>).map { rawItem ->
				val itemName = parseFlashString(rawItem, "playable character equipment")!!
				if (itemName == "none") null else inventoryAssets.items.find { it.flashName == itemName }!!
			},
			initialItems = (nestedCharacter[7] as ArrayList<ArrayList<String>>).map { rawStack ->
				val itemName = parseFlashString(rawStack[0], "playable character item")!!
				val amount = parseInt(rawStack[1])
				ItemStack(inventoryAssets.items.find { it.flashName == itemName }!!, amount)
			},
			initialMasteredSkills = masteredSkills,
			initialToggledSkills = toggledSkills,
		))
	}

	return characters
}

internal class CharacterParseException(message: String) : RuntimeException(message)
