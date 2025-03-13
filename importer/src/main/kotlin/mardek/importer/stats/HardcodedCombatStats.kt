package mardek.importer.stats

import mardek.content.combat.StatsContent
import mardek.content.combat.CombatStat

fun addCombatStats(assets: StatsContent) {
	val stats = arrayOf(
		"STR", "VIT", "SPR", "AGL",
		"ATK", "DEF", "MDEF", "evasion",
		"mp", "hp"
	)
	for (stat in stats) assets.stats.add(CombatStat(stat))
}
