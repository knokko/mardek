package mardek.importer.battle

import mardek.assets.battle.BattleAssets

internal fun importBattleAssets(): BattleAssets {
	val assets = BattleAssets()
	importBattleBackgrounds(assets)
	return assets
}
