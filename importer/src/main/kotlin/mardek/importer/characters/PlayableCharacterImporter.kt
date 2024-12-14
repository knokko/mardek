package mardek.importer.characters

import mardek.assets.area.AreaAssets
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatAssets
import mardek.assets.combat.StatModifier
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import mardek.state.ingame.inventory.ItemStack
import java.lang.Integer.parseInt

@Suppress("UNCHECKED_CAST")
internal fun importPlayableCharacters(
	combatAssets: CombatAssets,
	skillAssets: SkillAssets,
	inventoryAssets: InventoryAssets,
	areaAssets: AreaAssets,
	resourcePath: String
): List<FatPlayableCharacter> {
	val characters = ArrayList<FatPlayableCharacter>()
	val flashCode = parseActionScriptResource(resourcePath)

	val rawCharacters = flashCode.variableAssignments["PChatchery"]!!
	for ((key, rawCharacter) in parseActionScriptObject(rawCharacters)) {
		val prefix = "MakeCreatureStats("
		if (!rawCharacter.startsWith(prefix) || !rawCharacter.endsWith(")")) {
			throw CharacterParseException("Unexpected raw playable character $rawCharacter")
		}

		val nestedCharacter = parseActionScriptNestedList("[${rawCharacter.substring(prefix.length, rawCharacter.length - 1)}]") as ArrayList<*>
		println("key is $key and nested is $nestedCharacter")

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
			initialMasteredSkills = (nestedCharacter[8] as ArrayList<String>).map { rawActiveName ->
				val activeName = parseFlashString(rawActiveName, "playable character action name")!!
				playable.characterClass.skillClass.actions.find { it.name == activeName }!!
			} // TODO Passives and reactions
		))
	}

	return characters
}

internal class CharacterParseException(message: String) : RuntimeException(message)
