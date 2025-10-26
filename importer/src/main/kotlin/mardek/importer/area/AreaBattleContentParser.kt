package mardek.importer.area

import mardek.content.Content
import mardek.content.area.*
import mardek.content.battle.BattleContent
import mardek.content.battle.PartyLayout
import mardek.content.battle.PartyLayoutPosition
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import java.lang.Integer.parseInt
import java.util.*
import kotlin.collections.ArrayList

internal fun parseLevelRange(rawPair: String): LevelRange {
	val levelsList = parseActionScriptNestedList(rawPair)
	if (levelsList !is ArrayList<*> || levelsList.size != 2) throw AreaParseException("Unexpected level range $rawPair")
	return LevelRange(min = parseInt(levelsList[0].toString()), parseInt(levelsList[1].toString()))
}

private fun parsePositions(rawPositions: String): Array<PartyLayoutPosition> {
	val positionsList = parseActionScriptNestedList(rawPositions)
	if (positionsList !is ArrayList<*> || positionsList.size != 4) throw AreaParseException("Unexpected foe layout $rawPositions")

	return Array(4) { index ->
		val coordinateList = positionsList[index]
		if (coordinateList !is ArrayList<*> || coordinateList.size != 2) {
			throw AreaParseException("Unexpected foe position $coordinateList")
		}
		PartyLayoutPosition(parseInt(coordinateList[0].toString()), parseInt(coordinateList[1].toString()))
	}
}

internal fun parseSelections(battleContent: BattleContent, rawList: String): ArrayList<BattleEnemySelection> {
	val nestedList = parseActionScriptNestedList(rawList)
	if (nestedList !is ArrayList<*>) throw AreaParseException("Unexpected monster table entry $rawList")

	val selections = ArrayList<BattleEnemySelection>(nestedList.size)

	for (rawSelection in nestedList) {
		if (rawSelection !is ArrayList<*> || rawSelection.size != 5) {
			throw AreaParseException("Unexpected monster table entry $rawSelection")
		}
		val layout = battleContent.enemyPartyLayouts.find {
			it.name == parseFlashString(rawSelection[4] as String, "enemy party layout")!!
		}!!
		val monsters = rawSelection.subList(0, 4).map { rawName ->
			if (rawName == "null") null
			else {
				val monsterName = parseFlashString(rawName as String, "monster name")!!
				val monster = battleContent.monsters.find { it.name.lowercase(Locale.ROOT) == monsterName.lowercase(Locale.ROOT) }
				if (monster == null && battleContent.monsters.size <= 1) null else monster!!
			}
		}
		selections.add(BattleEnemySelection(ArrayList(monsters), layout))
	}

	return selections
}

fun importAreaBattleAssets(content: Content) {
	val code = parseActionScriptResource("mardek/importer/stats/monsters.txt")

	val levelsString = code.variableAssignments["MONSTER_LEVELS"]!!
	val rawLevels = parseActionScriptObject(levelsString)
	for ((name, rawPair) in rawLevels) {
		content.areas.levelRanges.add(SharedLevelRange(name, parseLevelRange(rawPair)))
	}

	val layoutsString = code.variableAssignments["foePartyLayouts"]!!
	val rawLayoutMap = parseActionScriptObject(layoutsString)
	for ((name, rawPositions) in rawLayoutMap) {
		content.battle.enemyPartyLayouts.add(PartyLayout(
			name, parsePositions(rawPositions),
			UUID.nameUUIDFromBytes("PartyLayout$name$rawPositions".encodeToByteArray()),
		))
	}

	if (content.battle.monsters.size > 1) { // In unit tests, the size <= 1
		val monstersString = code.variableAssignments["MONSTER_TABLES"]!!
		val rawMonsterTable = parseActionScriptObject(monstersString)
		for ((tableName, rawList) in rawMonsterTable) {
			content.areas.enemySelections.add(SharedEnemySelections(tableName, parseSelections(content.battle, rawList)))
		}
	}
}
