package mardek.importer.combat

import mardek.assets.combat.CombatAssets

fun importCombatAssets(): CombatAssets {
	val combatAssets = CombatAssets()
	addCombatStats(combatAssets)
	addElements(combatAssets)
	addStatusEffects(combatAssets)
	importRaces(combatAssets)
	return combatAssets
}
