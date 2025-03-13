package mardek.importer.stats

import mardek.content.Content

fun importStatsContent(content: Content) {
	addCombatStats(content.stats)
	addElements(content.stats)
	addStatusEffects(content.stats)
	importRaces(content.stats)
}
