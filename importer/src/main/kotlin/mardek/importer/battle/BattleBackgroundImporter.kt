package mardek.importer.battle

import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.FrameLabelTag
import com.jpexs.decompiler.flash.tags.PlaceObject2Tag
import java.io.File
import java.nio.file.Files

private fun extractFromFlash(swfFile: File) {
	val input = Files.newInputStream(swfFile.toPath())
	val swf = SWF(input, true)
	input.close()

	val backgroundsTag = swf.tags.find { it.uniqueId == "2186" }!! as DefineSpriteTag

	val shapeMapping = mutableMapOf<String, MutableList<Int>>()
	var currentLabel = ""
	for (tag in backgroundsTag.tags) {
		if (tag is FrameLabelTag) currentLabel = tag.labelName
		if (currentLabel.isNotEmpty() && currentLabel != "BELFAN" && tag is PlaceObject2Tag) {
			shapeMapping.computeIfAbsent(currentLabel) { mutableListOf() }.add(tag.characterId)
		}
	}
	println("mapping is $shapeMapping")

	val extractedShapesFolder = File("D:\\images/shapes")
	val destinationFolder = File("importer/src/main/resources/mardek/importer/battle/backgrounds")

	destinationFolder.mkdirs()

	for (entry in shapeMapping) {
		if (entry.value.size > 1) {
			println("do ${entry.key} manually")
			continue
		}
		val sourceFile = File("$extractedShapesFolder/${entry.value[0]}.png")
		val destinationFile = File("$destinationFolder/${entry.key}.png")
		if (destinationFile.exists()) destinationFile.delete()
		Files.copy(sourceFile.toPath(), destinationFile.toPath())
	}
}

fun main() {
	// Copy MARDEK.swf from Steam to ./flash/MARDEK.swf to make this work
	extractFromFlash(File("flash/MARDEK.swf"))
}
