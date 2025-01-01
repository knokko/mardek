package mardek.importer.area

import mardek.assets.area.AreaAssets
import mardek.assets.area.RandomAreaBattles
import mardek.assets.battle.BattleAssets
import mardek.importer.util.ActionScriptCode
import java.lang.Integer.parseInt

fun parseRandomBattle(areaCode: ActionScriptCode, battleAssets: BattleAssets, assets: AreaAssets): RandomAreaBattles? {
	val chance = try { parseInt(areaCode.variableAssignments["btlChance"]) } catch (complex: NumberFormatException) {
		println("failed to parse btlChance ${areaCode.variableAssignments["btlChance"]}")
		0
	}
	if (chance == 0) return null

	val minSteps = parseInt(areaCode.variableAssignments["minSteps"] ?: "0")

	val (ownLevelRange, sharedLevelRange) = run {
		val rawLevelRange = areaCode.variableAssignments["levelrange"]!!
		val levelRangePrefix = "MONSTER_LEVELS."
		if (rawLevelRange.startsWith(levelRangePrefix)) {
			val rangeName = rawLevelRange.substring(levelRangePrefix.length)
			Pair(null, assets.levelRanges.find { it.name == rangeName }!!)
		} else Pair(parseLevelRange(rawLevelRange), null)
	}

	val (ownEnemies, sharedEnemies) = run {
		val rawFoes = areaCode.variableAssignments["foes"]!!
		val monstersTablePrefix = "MONSTER_TABLES."
		if (rawFoes.startsWith(monstersTablePrefix)) {
			val tableName = rawFoes.substring(monstersTablePrefix.length)
			Pair(null, assets.enemySelections.find { it.name == tableName }!!)
		} else Pair(parseSelections(battleAssets, rawFoes), null)
	}

	val rawBackground = areaCode.variableAssignments["specBtlBG"]
	val specialBackground = if (rawBackground != null) {
		battleAssets.backgrounds.find { it.name == parseFlashString(rawBackground, "special battle background") }!!
	} else null

	val tileset = parseFlashString(areaCode.variableAssignments["tileset"]!!, "area tileset")
	return RandomAreaBattles(
		ownEnemies = ownEnemies,
		sharedEnemies = sharedEnemies,
		ownLevelRange = ownLevelRange,
		sharedLevelRange = sharedLevelRange,
		minSteps = minSteps,
		chance = chance,
		defaultBackground = battleAssets.backgrounds.find { it.name == tileset }!!,
		specialBackground = specialBackground
	)
}
