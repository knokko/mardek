package mardek.importer.area

import mardek.content.Content
import mardek.content.area.RandomAreaBattles
import mardek.content.area.SharedEnemySelections
import mardek.content.battle.BattleBackground
import mardek.importer.util.ActionScriptCode
import java.lang.Integer.parseInt
import java.util.Locale

fun parseRandomBattle(areaCode: ActionScriptCode, content: Content): RandomAreaBattles? {
	val chance = try { parseInt(areaCode.variableAssignments["btlChance"]) } catch (complex: NumberFormatException) {
		println("failed to parse btlChance ${areaCode.variableAssignments["btlChance"]}")
		0
	}
	if (chance == 0) return null

	// It looks like vanilla completely ignores the minSteps variable, and just assumes a hardcoded 30
	val minSteps = 30

	val (ownLevelRange, sharedLevelRange) = run {
		val rawLevelRange = areaCode.variableAssignments["levelrange"]!!
		val levelRangePrefix = "MONSTER_LEVELS."
		if (rawLevelRange.startsWith(levelRangePrefix)) {
			val rangeName = rawLevelRange.substring(levelRangePrefix.length)
			Pair(null, content.areas.levelRanges.find { it.name == rangeName }!!)
		} else Pair(parseLevelRange(rawLevelRange), null)
	}

	val (ownEnemies, sharedEnemies) = run {
		val rawFoes = areaCode.variableAssignments["foes"]!!
		val monstersTablePrefix = "MONSTER_TABLES."
		if (rawFoes.startsWith(monstersTablePrefix)) {
			val tableName = rawFoes.substring(monstersTablePrefix.length)
			val selection = content.areas.enemySelections.find { it.name == tableName }
			if (selection == null && content.battle.monsters.size <= 1) Pair(null, SharedEnemySelections())
			else Pair(null, selection!!)
		} else Pair(parseSelections(content.battle, rawFoes), null)
	}

	val rawBackground = areaCode.variableAssignments["specBtlBG"]
	val specialBackground = if (rawBackground != null) {
		content.battle.backgrounds.find { it.name == parseFlashString(rawBackground, "special battle background") }!!
	} else null

	val tileset = parseFlashString(
		areaCode.variableAssignments["tileset"]!!, "area tileset"
	)!!.lowercase(Locale.ROOT)

	// The battle background list will be empty during some unit tests
	val defaultBackground = if (content.battle.backgrounds.isEmpty()) BattleBackground(tileset, emptyArray(), 1)
	else content.battle.backgrounds.find { it.name == tileset }!!

	return RandomAreaBattles(
		ownEnemies = ownEnemies,
		sharedEnemies = sharedEnemies,
		ownLevelRange = ownLevelRange,
		sharedLevelRange = sharedLevelRange,
		minSteps = minSteps,
		chance = chance,
		defaultBackground = defaultBackground,
		specialBackground = specialBackground
	)
}
