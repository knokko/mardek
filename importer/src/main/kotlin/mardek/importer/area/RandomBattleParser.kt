package mardek.importer.area

import mardek.assets.area.BattleEnemySelection
import mardek.assets.area.RandomAreaBattles
import java.lang.Integer.parseInt

fun parseRandomBattle(parsing: ParsingArea1): RandomAreaBattles? {
	val chance = parseInt(parsing.variableAssignments["btlChance"])
	if (chance == 0) return null

	val minSteps = parseInt(parsing.variableAssignments["minSteps"] ?: "0")

	val (ownLevelRange, levelRangeName) = run {
		val rawLevelRange = parsing.variableAssignments["levelrange"]!!
		val levelRangePrefix = "MONSTER_LEVELS."
		if (rawLevelRange.startsWith(levelRangePrefix)) return@run Pair(null, rawLevelRange.substring(levelRangePrefix.length))

		parseAssert(rawLevelRange.startsWith("["), "Expected levelrange $rawLevelRange to start with [")
		parseAssert(rawLevelRange.endsWith("]"), "Expected levelrange $rawLevelRange to end with ]")

		val innerLevelRange = rawLevelRange.substring(1, rawLevelRange.length - 1)

		val splitLevelRange = innerLevelRange.split(",")
		parseAssert(splitLevelRange.size == 2, "Expected levelrange $rawLevelRange to be split in 2 parts by ,")

		Pair(Pair(parseInt(splitLevelRange[0]), parseInt(splitLevelRange[1])), null)
	}

	val (ownEnemies, monstersTableName) = run {
		val rawFoes = parsing.variableAssignments["foes"]!!
		val monstersTablePrefix = "MONSTER_TABLES."
		if (rawFoes.startsWith(monstersTablePrefix)) return@run Pair(null, rawFoes.substring(monstersTablePrefix.length))

		parseAssert(rawFoes.startsWith("[["), "Expected foes $rawFoes to start with [[")
		parseAssert(rawFoes.endsWith("]]"), "Expected foes $rawFoes to end with ]]")

		val innerFoes = rawFoes.substring(2, rawFoes.length - 2)
		val splitFoes = innerFoes.split("],[")

		val ownEnemies = mutableListOf<BattleEnemySelection>()
		for (rawSelection in splitFoes) {
			val selectionSplit = rawSelection.split(",")
			parseAssert(selectionSplit.size == 5, "Expected $selectionSplit to have a size of 5")

			val selectionName = parseFlashString(selectionSplit[4], "random battle selection")

			val enemyNames = selectionSplit.subList(0, 4).map {
				if (it == "null") null else parseFlashString(it, "monster name")
			}

			ownEnemies.add(BattleEnemySelection(selectionName!!, enemyNames))
		}

		Pair(ownEnemies, null)
	}

	val rawBackground = parsing.variableAssignments["specBtlBG"]
	val specialBackground = if (rawBackground != null) {
		parseFlashString(rawBackground, "special battle background")
	} else null

	return RandomAreaBattles(
		ownEnemies = ownEnemies,
		monstersTableName = monstersTableName,
		ownLevelRange = ownLevelRange,
		levelRangeName = levelRangeName,
		minSteps = minSteps,
		chance = chance,
		specialBackground = specialBackground
	)
}
