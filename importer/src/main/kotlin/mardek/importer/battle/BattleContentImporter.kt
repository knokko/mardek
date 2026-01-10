package mardek.importer.battle

import mardek.content.Content
import java.awt.image.BufferedImage

internal fun importBattleContent(content: Content, importMonsters: Boolean) {
	if (importMonsters) {
		importBattleBackgrounds(content.battle)
		importMonsters(content, null)
	}
	importLootTexts(content.battle)

	val noMask = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
	noMask.setRGB(0, 0, -1)
	content.battle.noMask.bufferedImage = noMask
}
