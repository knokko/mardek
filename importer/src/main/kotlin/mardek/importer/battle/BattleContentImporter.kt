package mardek.importer.battle

import mardek.content.Content
import mardek.content.animations.BattleModel

internal fun importBattleContent(content: Content, playerModelMapping: MutableMap<String, BattleModel>?) {
	importBattleBackgrounds(content.battle)
	if (playerModelMapping != null) importMonsters(content, playerModelMapping)
}
