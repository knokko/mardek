package mardek.importer.encyclopedia

import mardek.content.Content
import mardek.content.encyclopedia.EncyclopediaPerson
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionEncyclopediaPersonValue
import mardek.content.expression.ExpressionIntValue
import mardek.content.expression.SwitchCaseStateExpression
import mardek.content.portrait.PortraitInfo
import mardek.importer.area.parseFlashString
import java.lang.Integer.parseInt
import java.util.TreeMap
import java.util.UUID

internal fun importEncyclopediaPeople(content: Content, skipPortraits: Boolean) {
	importSomeEncyclopediaContent(content, "People") { peopleList, chapterExpression, _ ->
		val nameMapping = mutableMapOf<String, MutableList<Map<String, String>>>()
		for (rawPersonEntry in peopleList) {
			val name = parseFlashString(rawPersonEntry["name"]!!, "encyclopedia person name")!!
			nameMapping.computeIfAbsent(name) { mutableListOf() }.add(rawPersonEntry)
		}

		for ((name, rawSnapshots) in nameMapping) {
			val snapshotMapping = TreeMap<Int, EncyclopediaPerson.Snapshot>()
			for (rawSnapshot in rawSnapshots) {
				val firstChapter = parseInt(rawSnapshot["CH"]!!)
				val lastChapter = if (rawSnapshot.containsKey("LastChapter")) {
					parseInt(rawSnapshot["LastChapter"])
				} else 3

				if (firstChapter > lastChapter) throw RuntimeException("Unused snapshot: $rawSnapshot")

				val rawAge = rawSnapshot["age"]
				val initialAge = if (rawAge != null && rawAge.endsWith(" + _loc2_")) {
					parseInt(rawAge.substring(0 until rawAge.indexOf(" +")))
				} else null
				val overrideAge = if (rawSnapshot.containsKey("fixedAge")) parseFlashString(
					rawSnapshot["fixedAge"]!!, "encyclopedia person overrideAge"
				)!! else null

				val portraitModel = parseFlashString(
					rawSnapshot["model"]!!, "encyclopedia model/portrait"
				)!!
				val characterClasses = content.stats.classes.filter { it.displayName == parseFlashString(
					rawSnapshot["Class"]!!, "encyclopedia person class"
				)!! }
				val characterClass = if (characterClasses.size == 1) characterClasses[0]
				else characterClasses.find { it.rawName == portraitModel }!!

				val portrait = if (skipPortraits) PortraitInfo() else {
					content.portraits.info.find { it.flashName.equals(portraitModel, ignoreCase = true) }!!
				}
				val snapshot = EncyclopediaPerson.Snapshot(
					firstName = name,
					lastName = parseFlashString(
						rawSnapshot["lastname"]!!, "encyclopedia lastname"
					)!!,
					portrait = portrait,
					portraitExpression = if (rawSnapshot.containsKey("em")) {
						parseFlashString(rawSnapshot["em"]!!, "encyclopedia expression")!!
					} else "norm",
					creatureType = content.stats.creatureTypes.find { it.niceName == parseFlashString(
						rawSnapshot["race"]!!, "encyclopedia person race"
					)!! }!!,
					gender = EncyclopediaPerson.Gender.valueOf(parseFlashString(
						rawSnapshot["gender"]!!, "encyclopedia person gender"
					)!!),
					initialAge = initialAge,
					overrideAge = overrideAge,
					origin = parseFlashString(
						rawSnapshot["origin"]!!, "encyclopedia person origin"
					)!!,
					weaponType = content.items.itemTypes.find { it.niceName == parseFlashString(
						rawSnapshot["weapon"]!!, "encyclopedia person weapon type"
					)!! }!!,
					element = content.stats.elements.find { it.rawName == parseFlashString(
						rawSnapshot["elem"]!!, "encyclopedia person element"
					)!! }!!,
					characterClass = characterClass,
					alignment = EncyclopediaPerson.Alignment.entries.find { it.displayName == parseFlashString(
						rawSnapshot["alignment"]!!, "encyclopedia person alignment"
					)!! }!!,
					description = parseFlashString(
						rawSnapshot["bio"]!!, "encyclopedia person description"
					)!!
				)

				for (chapter in firstChapter .. lastChapter) {
					if (snapshotMapping.containsKey(chapter)) {
						throw RuntimeException("Double snapshot $rawSnapshots")
					}
					snapshotMapping[chapter] = snapshot
				}
			}

			val chooseSnapshot = if (snapshotMapping.keys.size == 3 && snapshotMapping.values.toSet().size == 1) {
				ConstantStateExpression(ExpressionEncyclopediaPersonValue(snapshotMapping[1]!!))
			} else SwitchCaseStateExpression(
				input = chapterExpression,
				cases = snapshotMapping.map { (chapter, snapshot) ->
					SwitchCaseStateExpression.Case(
						inputToMatch = ConstantStateExpression(ExpressionIntValue(chapter)),
						outputWhenInputMatches = ConstantStateExpression(
							ExpressionEncyclopediaPersonValue(snapshot)
						)
					)
				}.toTypedArray(),
				defaultOutput = ConstantStateExpression(ExpressionEncyclopediaPersonValue(null)),
			)

			content.encyclopedia.people.add(EncyclopediaPerson(
				id = UUID.nameUUIDFromBytes("EncyclopediaPerson$name".encodeToByteArray()),
				snapshots = snapshotMapping.values.toSet().toTypedArray(),
				chooseSnapshot = chooseSnapshot,
			))
		}
	}
}
