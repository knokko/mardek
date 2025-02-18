package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.BattleAssets
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets

internal fun importBattleAssets(
	combatAssets: CombatAssets, itemAssets: InventoryAssets, skillAssets: SkillAssets,
	playerModelMapping: MutableMap<String, BattleModel>?
): BattleAssets {
	val assets = BattleAssets()
	importBattleBackgrounds(assets)
	if (playerModelMapping != null) importMonsters(combatAssets, itemAssets, skillAssets, assets, playerModelMapping)
	return assets
}
