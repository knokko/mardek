package mardek.importer.combat

import mardek.assets.combat.CharacterRace
import mardek.assets.combat.CombatAssets
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptResource
import java.lang.RuntimeException

fun importRaces(combatAssets: CombatAssets) {
	val monsterData = parseActionScriptResource("mardek/importer/combat/monsters.txt")
	var raceList = monsterData.variableAssignments["MonsterTypes"]!!
	if (!raceList.startsWith("[") || !raceList.endsWith("]")) {
		throw RuntimeException("Expected $raceList to be surrounded by []")
	}

	raceList = raceList.substring(1 until raceList.length - 1)
	for (race in raceList.split(",")) {
		combatAssets.races.add(CharacterRace(parseFlashString(race, "race name")!!))
	}
}
