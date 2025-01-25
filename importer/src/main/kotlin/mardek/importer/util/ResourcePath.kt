package mardek.importer.util

import java.io.File

private fun determineResourcesFolder(): File {
	return if (File("importer").isDirectory) File("importer/src/main/resources/mardek/importer")
	else File("src/main/resources/mardek/importer")
}

val resourcesFolder = determineResourcesFolder()
val projectFolder = resourcesFolder.parentFile.parentFile.parentFile.parentFile.absoluteFile.parentFile.parentFile
