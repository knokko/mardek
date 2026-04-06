package mardek.importer.encyclopedia

import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import mardek.content.Content
import mardek.content.animation.AnimationFrames
import mardek.content.animation.StandaloneAnimation
import mardek.content.encyclopedia.EncyclopediaArtefact
import mardek.importer.animation.AnimationImportContext
import mardek.importer.animation.findDependencies
import mardek.importer.animation.importSkinnedAnimation
import mardek.importer.area.FLASH
import mardek.importer.area.parseFlashString
import mardek.importer.util.projectFolder
import java.io.File
import java.util.UUID

internal fun importEncyclopediaArtefacts(content: Content, skipAnimations: Boolean) {
	val magicScale = 2
	importSomeEncyclopediaContent(content, "Artefacts") { artefactsList, _, shouldShowUp ->
		val animations = if (skipAnimations) {
			arrayOf(
				"Fire Crystal", "Water Crystal", "Earth Crystal", "Air Crystal",
				"Light Crystal", "Dark Crystal", "Ether Crystal",
				"Dreamstones", "Miasmal Chalice",
			).map { Pair(it, StandaloneAnimation()) }
		} else {
			val artefactsTag = FLASH.tags.find { it.uniqueId == "2221" }!! as DefineSpriteTag
			val context = AnimationImportContext(
				shapesDirectory = File("$projectFolder/flash/all-shapes-x${magicScale}/"),
				particleEmitters = emptyMap(),
			)
			val artefactsAnimations = importSkinnedAnimation(artefactsTag, context)
			artefactsAnimations.skins.map { (name, animation) ->
				val frames = AnimationFrames(animation.get().frames)
				val (innerSprites, innerAnimations) = findDependencies(
					frames.frames.flatMap { it.nodes.toList() }
				)
				Pair(name, StandaloneAnimation(
					frames, innerSprites, innerAnimations, emptyArray(), emptyArray()
				))
			}
		}

		for (rawArtefactEntry in artefactsList) {
			val name = parseFlashString(rawArtefactEntry["name"]!!, "encyclopedia artefact name")!!
			val description = parseFlashString(
				rawArtefactEntry["info"]!!, "encyclopedia artefact description"
			)!!

			val element = content.stats.elements.find { it.rawName == parseFlashString(
				rawArtefactEntry["elem"]!!, "encyclopedia artefact element"
			)!! }!!

			content.encyclopedia.artefacts.add(EncyclopediaArtefact(
				id = UUID.nameUUIDFromBytes("EncyclopediaArtefact$name".encodeToByteArray()),
				name = name,
				description = description,
				element = element,
				animation = animations.find { it.first.equals(name, ignoreCase = true) }!!.second,
				magicScale = magicScale,
				shouldShowUp = shouldShowUp(rawArtefactEntry),
			))
		}
	}
}
