package mardek.importer.util

import mardek.importer.area.SpritesAndAreas
import java.io.File

private fun determineResourcesFolder(): File {
	return if (File("importer").isDirectory) File("importer/src/main/resources/mardek/importer")
	else File("src/main/resources/mardek/importer")
}

val resourcesFolder = determineResourcesFolder()
val projectFolder = resourcesFolder.parentFile.parentFile.parentFile.parentFile.absoluteFile.parentFile.parentFile!!
val classLoader = SpritesAndAreas::class.java.classLoader!!
