package mardek.importer.stats

import mardek.content.stats.StatsContent
import mardek.content.stats.CombatStat

fun addCombatStats(assets: StatsContent) {
	val stats = arrayOf(
		"STR", "VIT", "SPR", "AGL",
		"ATK", "DEF", "MDEF", "evasion",
		"mp", "hp"
	)
	for (stat in stats) assets.stats.add(CombatStat(stat))
}
