package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.BattleAssets
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets

internal fun importBattleAssets(
	combatAssets: CombatAssets, itemAssets: InventoryAssets,
	playerModelMapping: MutableMap<String, BattleModel>?
): BattleAssets {
	val assets = BattleAssets()
	importBattleBackgrounds(assets)
	if (playerModelMapping != null) importMonsters(combatAssets, itemAssets, assets, playerModelMapping)
	return assets
}
