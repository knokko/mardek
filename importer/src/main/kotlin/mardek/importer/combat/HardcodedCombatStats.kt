package mardek.importer.combat

import mardek.assets.combat.CombatAssets
import mardek.assets.combat.CombatStat

fun addCombatStats(assets: CombatAssets) {
	val stats = arrayOf(
		"STR", "VIT", "SPR", "AGL",
		"ATK", "DEF", "MDEF", "evasion",
		"mp", "hp"
	)
	for (stat in stats) assets.stats.add(CombatStat(stat))
}
