package mardek.importer.area

import mardek.assets.area.BattleEnemySelection
import mardek.assets.area.LevelRange
import mardek.assets.area.RandomAreaBattles
import mardek.assets.battle.BattleAssets
import mardek.importer.util.ActionScriptCode
import java.lang.Integer.parseInt

fun parseRandomBattle(areaCode: ActionScriptCode, battleAssets: BattleAssets): RandomAreaBattles? {
	val chance = try { parseInt(areaCode.variableAssignments["btlChance"]) } catch (complex: NumberFormatException) {
		println("failed to parse btlChance ${areaCode.variableAssignments["btlChance"]}")
		0
	}
	if (chance == 0) return null

	val minSteps = parseInt(areaCode.variableAssignments["minSteps"] ?: "0")

	val (ownLevelRange, levelRangeName) = run {
		val rawLevelRange = areaCode.variableAssignments["levelrange"]!!
		val levelRangePrefix = "MONSTER_LEVELS."
		if (rawLevelRange.startsWith(levelRangePrefix)) return@run Pair(null, rawLevelRange.substring(levelRangePrefix.length))

		parseAssert(rawLevelRange.startsWith("["), "Expected levelrange $rawLevelRange to start with [")
		parseAssert(rawLevelRange.endsWith("]"), "Expected levelrange $rawLevelRange to end with ]")

		val innerLevelRange = rawLevelRange.substring(1, rawLevelRange.length - 1)

		val splitLevelRange = innerLevelRange.split(",")
		parseAssert(splitLevelRange.size == 2, "Expected levelrange $rawLevelRange to be split in 2 parts by ,")

		Pair(LevelRange(parseInt(splitLevelRange[0]), parseInt(splitLevelRange[1])), null)
	}

	val (ownEnemies, monstersTableName) = run {
		val rawFoes = areaCode.variableAssignments["foes"]!!
		val monstersTablePrefix = "MONSTER_TABLES."
		if (rawFoes.startsWith(monstersTablePrefix)) return@run Pair(null, rawFoes.substring(monstersTablePrefix.length))

		parseAssert(rawFoes.startsWith("[["), "Expected foes $rawFoes to start with [[")
		parseAssert(rawFoes.endsWith("]]"), "Expected foes $rawFoes to end with ]]")

		val innerFoes = rawFoes.substring(2, rawFoes.length - 2)
		val splitFoes = innerFoes.split("],[")

		val ownEnemies = ArrayList<BattleEnemySelection>(splitFoes.size)
		for (rawSelection in splitFoes) {
			val selectionSplit = rawSelection.split(",")
			parseAssert(selectionSplit.size == 5, "Expected $selectionSplit to have a size of 5")

			val selectionName = parseFlashString(selectionSplit[4], "random battle selection")

			val enemyNames = ArrayList(selectionSplit.subList(0, 4).map {
				if (it == "null") null else parseFlashString(it, "monster name")
			})

			ownEnemies.add(BattleEnemySelection(selectionName!!, enemyNames))
		}

		Pair(ownEnemies, null)
	}

	val rawBackground = areaCode.variableAssignments["specBtlBG"]
	val specialBackground = if (rawBackground != null) {
		battleAssets.backgrounds.find { it.name == parseFlashString(rawBackground, "special battle background") }!!
	} else null

	val tileset = parseFlashString(areaCode.variableAssignments["tileset"]!!, "area tileset")
	return RandomAreaBattles(
		ownEnemies = ownEnemies,
		monstersTableName = monstersTableName,
		ownLevelRange = ownLevelRange,
		levelRangeName = levelRangeName,
		minSteps = minSteps,
		chance = chance,
		defaultBackground = battleAssets.backgrounds.find { it.name == tileset }!!,
		specialBackground = specialBackground
	)
}
