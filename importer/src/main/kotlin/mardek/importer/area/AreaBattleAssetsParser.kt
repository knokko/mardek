package mardek.importer.area

import mardek.assets.area.*
import mardek.assets.battle.BattleAssets
import mardek.assets.battle.PartyLayout
import mardek.assets.battle.PartyLayoutPosition
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptResource
import java.lang.Integer.parseInt

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

internal fun parseSelections(battleAssets: BattleAssets, rawList: String): ArrayList<BattleEnemySelection> {
	val nestedList = parseActionScriptNestedList(rawList)
	if (nestedList !is ArrayList<*>) throw AreaParseException("Unexpected monster table entry $rawList")

	val selections = ArrayList<BattleEnemySelection>(nestedList.size)

	for (rawSelection in nestedList) {
		if (rawSelection !is ArrayList<*> || rawSelection.size != 5) {
			throw AreaParseException("Unexpected monster table entry $rawSelection")
		}
		val layout = battleAssets.enemyPartyLayouts.find {
			it.name == parseFlashString(rawSelection[4] as String, "enemy party layout")!!
		}!!
		val monsterNames = rawSelection.subList(0, 4).map { rawName ->
			if (rawName == "null") null
			else parseFlashString(rawName as String, "monster name")!!
		}
		selections.add(BattleEnemySelection(ArrayList(monsterNames), layout))
	}

	return selections
}

fun importAreaBattleAssets(battleAssets: BattleAssets, assets: AreaAssets) {
	val code = parseActionScriptResource("mardek/importer/combat/monsters.txt")

	val levelsString = code.variableAssignments["MONSTER_LEVELS"]!!
	val rawLevels = parseActionScriptObject(levelsString)
	for ((name, rawPair) in rawLevels) {
		assets.levelRanges.add(SharedLevelRange(name, parseLevelRange(rawPair)))
	}

	val layoutsString = code.variableAssignments["foePartyLayouts"]!!
	val rawLayoutMap = parseActionScriptObject(layoutsString)
	for ((name, rawPositions) in rawLayoutMap) {
		battleAssets.enemyPartyLayouts.add(PartyLayout(name, parsePositions(rawPositions)))
	}

	// TODO Test this
	val monstersString = code.variableAssignments["MONSTER_TABLES"]!!
	val rawMonsterTable = parseActionScriptObject(monstersString)
	for ((tableName, rawList) in rawMonsterTable) {
		assets.enemySelections.add(SharedEnemySelections(tableName, parseSelections(battleAssets, rawList)))
	}
}
