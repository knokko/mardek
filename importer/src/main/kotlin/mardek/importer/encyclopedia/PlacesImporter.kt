package mardek.importer.encyclopedia

import mardek.content.Content
import mardek.content.battle.BattleBackground
import mardek.content.encyclopedia.EncyclopediaArea
import mardek.importer.area.parseFlashString
import java.util.UUID

internal fun importEncyclopediaPlaces(content: Content, skipBackgrounds: Boolean) {
	importSomeEncyclopediaContent(content, "Places") { placesList, _, shouldShowUp ->
		for (rawAreaEntry in placesList) {
			val name = parseFlashString(rawAreaEntry["name"]!!, "encyclopedia area name")!!
			val description = parseFlashString(
				rawAreaEntry["info"]!!, "encyclopedia area description"
			)!!

			val backgroundName = parseFlashString(
				rawAreaEntry["tileset"]!!, "encyclopedia area background"
			)!!
			val background = if (skipBackgrounds) BattleBackground() else content.battle.backgrounds.find {
				it.name.equals(backgroundName, ignoreCase = true)
			}!!

			content.encyclopedia.places.add(EncyclopediaArea(
				id = UUID.nameUUIDFromBytes("EncyclopediaArea$name".encodeToByteArray()),
				name = name,
				description = description,
				background = background,
				shouldShowUp = shouldShowUp(rawAreaEntry),
			))
		}
	}
}
