package mardek.importer.stats

import mardek.content.stats.CreatureType
import mardek.content.stats.StatsContent
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptResource
import java.lang.RuntimeException

internal fun importRaces(statsContent: StatsContent) {
	val monsterData = parseActionScriptResource("mardek/importer/stats/monsters.txt")
	var raceList = monsterData.variableAssignments["MonsterTypes"]!!
	if (!raceList.startsWith("[") || !raceList.endsWith("]")) {
		throw RuntimeException("Expected $raceList to be surrounded by []")
	}

	raceList = raceList.substring(1 until raceList.length - 1)
	for (race in raceList.split(",")) {
		statsContent.creatureTypes.add(CreatureType(parseFlashString(race, "race name")!!))
	}
}
