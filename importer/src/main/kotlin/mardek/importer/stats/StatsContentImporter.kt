package mardek.importer.stats

import mardek.content.Content

fun importStatsContent(content: Content) {
	addElements(content)
	addStatusEffects(content.stats)
	importCreatureTypes(content.stats)
}
