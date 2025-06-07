package mardek.importer.stats

import mardek.content.stats.CreatureType
import mardek.content.stats.StatsContent
import mardek.importer.area.parseFlashString
import mardek.importer.ui.BcPacker
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptResource
import javax.imageio.ImageIO

internal fun importCreatureTypes(statsContent: StatsContent) {
	val monsterData = parseActionScriptResource("mardek/importer/stats/monsters.txt")
	val spriteSheet = ImageIO.read(BcPacker::class.java.classLoader.getResource(
		"mardek/importer/stats/CreatureTypeIcons.png"
	))
	val rawCreatureTypeList = parseActionScriptNestedList(monsterData.variableAssignments["MonsterTypes"]!!)
	val creatureTypeList = (rawCreatureTypeList as ArrayList<*>).map {
		parseFlashString(it.toString(), "creature type name")!!
	}

	for ((index, typeName) in creatureTypeList.withIndex()) {
		statsContent.creatureTypes.add(CreatureType(
			flashName = typeName,
			icon = compressKimSprite1(spriteSheet.getSubimage(16 * index, 0, 16, spriteSheet.height)),
			revertsHealing = typeName == "UNDEAD",
		))
	}
}
