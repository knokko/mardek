package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.BattleAssets

internal fun importBattleAssets(playerModelMapping: MutableMap<String, BattleModel>?): BattleAssets {
	val assets = BattleAssets()
	importBattleBackgrounds(assets)
	if (playerModelMapping != null) importMonsters(assets, playerModelMapping)
	return assets
}
