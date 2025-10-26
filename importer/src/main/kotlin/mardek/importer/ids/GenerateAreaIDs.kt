package mardek.importer.ids

import mardek.importer.util.resourcesFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID

fun main() {
	val areaDirectory = File("$resourcesFolder/area/data")
	for (areaFile in areaDirectory.listFiles()!!) {
		val stringContent = Files.readString(areaFile.toPath())
		if (stringContent.contains("uuid = ")) continue

		Files.write(areaFile.toPath(), "uuid = ${UUID.randomUUID()};\n".encodeToByteArray(), StandardOpenOption.APPEND)
	}
}
