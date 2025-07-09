package mardek.importer.battle

import mardek.content.Content
import mardek.content.animation.CombatantAnimations
import java.awt.image.BufferedImage

internal fun importBattleContent(content: Content, playerModelMapping: MutableMap<String, CombatantAnimations>?) {
	if (playerModelMapping != null) {
		importBattleBackgrounds(content.battle)
		importMonsters(content, playerModelMapping)
	}
	importLootTexts(content.battle)

	val noMask = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
	noMask.setRGB(0, 0, -1)
	content.battle.noMask.bufferedImage = noMask
}
