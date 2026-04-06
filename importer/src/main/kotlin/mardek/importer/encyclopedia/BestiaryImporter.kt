package mardek.importer.encyclopedia

import mardek.content.Content
import mardek.content.encyclopedia.EncyclopediaMonster
import mardek.importer.area.parseFlashString
import java.lang.Integer.parseInt

internal fun importBestiary(content: Content) {
	importSomeEncyclopediaContent(content, "Bestiary") { bestiaryList, _, shouldShowUp ->
		for (rawMonsterEntry in bestiaryList) {
			val monsterName = parseFlashString(
				rawMonsterEntry["name"]!!, "Encyclopedia monster name"
			)!!
			val monsterName2 = if (rawMonsterEntry.contains("model")) parseFlashString(
				rawMonsterEntry["model"]!!, "Encyclopedia monster model"
			) else null
			val displayName = if (rawMonsterEntry.contains("displayName")) parseFlashString(
				rawMonsterEntry["displayName"]!!, "Encyclopedia monster display name"
			) else null
			val elementName = parseFlashString(
				rawMonsterEntry["elem"]!!, "Encyclopedia monster element"
			)!!
			val description = parseFlashString(
				rawMonsterEntry["info"]!!, "Encyclopedia monster description"
			)!!
			val expectedNumMonsters = if (rawMonsterEntry.contains("amount")) {
				parseInt(rawMonsterEntry["amount"])
			} else 1

			val monsters = content.battle.monsters.filter {
				if (displayName != null) it.displayName.equals(displayName, ignoreCase = true)
				else if (monsterName2 == null) it.name.equals(monsterName, ignoreCase = true)
				else it.name.equals(monsterName2, ignoreCase = true)
			}

			if (monsters.isEmpty()) throw RuntimeException(
				"Can't find monster $monsterName / $monsterName2 / $displayName / $elementName for encyclopedia"
			)
			if (monsters.size != expectedNumMonsters) throw RuntimeException(
				"Wrong #monsters match $monsterName / $monsterName2 / $displayName / $elementName: " +
						"expected $expectedNumMonsters, but got $monsters"
			)

			if (monsters[0].element.rawName != elementName) {
				throw RuntimeException(
					"Expected encyclopedia monster $monsterName ($monsterName2) / $displayName) " +
							"to have element $elementName, but found ${monsters[0].element.rawName}"
				)
			}

			content.encyclopedia.monsters.add(EncyclopediaMonster(
				monsters = monsters.toTypedArray(),
				description = description,
				shouldShowUp = shouldShowUp(rawMonsterEntry),
			))
		}
	}
}
