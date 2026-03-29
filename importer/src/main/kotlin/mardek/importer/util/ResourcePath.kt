package mardek.importer.util

import mardek.content.Content
import mardek.importer.area.ParsedArea
import java.io.File

val projectFolder = Content.RESOURCES_DIRECTORY.parentFile!!
val resourcesFolder = File("$projectFolder/importer/src/main/resources/mardek/importer")
val classLoader = ParsedArea::class.java.classLoader!!
