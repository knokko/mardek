package mardek.importer.combat

import mardek.assets.combat.CreatureType
import mardek.assets.combat.CombatAssets
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptResource
import java.lang.RuntimeException

internal fun importRaces(combatAssets: CombatAssets) {
	val monsterData = parseActionScriptResource("mardek/importer/combat/monsters.txt")
	var raceList = monsterData.variableAssignments["MonsterTypes"]!!
	if (!raceList.startsWith("[") || !raceList.endsWith("]")) {
		throw RuntimeException("Expected $raceList to be surrounded by []")
	}

	raceList = raceList.substring(1 until raceList.length - 1)
	for (race in raceList.split(",")) {
		combatAssets.races.add(CreatureType(parseFlashString(race, "race name")!!))
	}
}
