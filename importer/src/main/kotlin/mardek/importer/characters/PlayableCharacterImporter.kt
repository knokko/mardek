package mardek.importer.characters

import mardek.content.Content
import mardek.content.animation.CombatantAnimations
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.StatModifier
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import mardek.content.portrait.PortraitInfo
import mardek.content.stats.CombatStat
import java.lang.Integer.parseInt
import java.util.UUID

@Suppress("UNCHECKED_CAST")
internal fun importPlayableCharacters(
	content: Content, playerModelMapping: Map<String, CombatantAnimations>?
) {
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
		val areaSpritesName = parseFlashString(
			nestedCharacter[1] as String, "playable character area sprites"
		)!!
		content.playableCharacters.add(PlayableCharacter(
			name = name,
			characterClass = content.stats.classes.find { it.rawName == className }!!,
			element = content.stats.elements.find {
				it.rawName == parseFlashString(nestedCharacter[3] as String, "playable character element")!!
			}!!,
			baseStats = statMods,
			areaSprites = content.areas.characterSprites.find { it.name == areaSpritesName }!!,
			animations = if (playerModelMapping != null) playerModelMapping[className]!! else CombatantAnimations(),
			creatureType = content.stats.creatureTypes.find { it.flashName == "HUMAN" }!!,
			portraitInfo = if (playerModelMapping != null) {
				content.portraits.info.find { it.flashName == areaSpritesName }!!
			} else PortraitInfo(),
			id = UUID.nameUUIDFromBytes("PlayableCharacterImporter$name$className".encodeToByteArray()),
		))
	}
}

internal class CharacterParseException(message: String) : RuntimeException(message)
