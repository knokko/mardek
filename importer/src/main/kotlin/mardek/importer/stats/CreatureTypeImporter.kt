package mardek.importer.stats

import mardek.content.stats.CreatureType
import mardek.content.stats.StatsContent
import mardek.importer.area.parseFlashString
import mardek.importer.util.classLoader
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal fun importCreatureTypes(statsContent: StatsContent) {
	val monsterData = parseActionScriptResource("mardek/importer/stats/monsters.txt")
	val spriteSheet = ImageIO.read(classLoader.getResource(
		"mardek/importer/stats/CreatureTypeIcons.png"
	))
	val rawCreatureTypeList = parseActionScriptNestedList(monsterData.variableAssignments["MonsterTypes"]!!)
	val creatureTypeList = (rawCreatureTypeList as ArrayList<*>).map {
		parseFlashString(it.toString(), "creature type name")!!
	}

	for ((index, typeName) in creatureTypeList.withIndex()) {
		var niceName = ""
		if (typeName == "HUMAN") niceName = "Human"
		if (typeName == "REPTOID") niceName = "Reptoid"
		if (typeName == "ANNUNAKI") niceName = "Annunaki"
		if (typeName == "CONSTRUCT") niceName = "Construct"
		statsContent.creatureTypes.add(CreatureType(
			flashName = typeName,
			icon = compressKimSprite3(spriteSheet.getSubimage(16 * index, 0, 16, spriteSheet.height)),
			revertsHealing = typeName == "UNDEAD",
			niceName = niceName,
		))
	}

	// This one is for Solaar
	// TODO CHAP3 Make sure Slenck, Solaar, and Legion get the right creature type (not HUMAN)
	statsContent.creatureTypes.add(CreatureType(
		flashName = "ARUAN",
		icon = compressKimSprite3(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)),
		revertsHealing = false,
		niceName = "Aruan"
	))
}
