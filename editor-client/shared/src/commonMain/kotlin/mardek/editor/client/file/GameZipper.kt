package mardek.editor.client.file

import mardek.content.Content
import mardek.content.ContentWrapper
import mardek.editor.MardekOS
import mardek.editor.util.includeDirectory
import mardek.editor.util.includeExtractedZipFile
import mardek.editor.util.includeFile
import mardek.editor.util.writeTextFile
import mardek.exporting.exportAndPrepareVk2d
import mardek.exporting.exportIcons
import mardek.exporting.exportTitleScreen
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

fun zipGame(contentWrapper: ContentWrapper, outputFile: File, os: MardekOS) {
	println("Zipping game executable to ${outputFile.absolutePath}")

	val gameJar = File("${Content.RESOURCES_DIRECTORY}/mardek.jar")
	val musicFolder = File("${Content.RESOURCES_DIRECTORY}/music")
	val gameWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/java-runtime-windows-x64.zip")
	val gameLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/java-runtime-linux-x64.zip")

	val zipOutput = ZipArchiveOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	val prefix = "/mardek-$os"
	includeFile(zipOutput, gameJar, "$prefix/resources/mardek.jar")
	includeDirectory(zipOutput, musicFolder, "$prefix/resources/music/")

	if (os == MardekOS.Windows) {
		includeExtractedZipFile(zipOutput, gameWindowsRuntime, "$prefix/java-runtime/")
		writeTextFile(zipOutput, "$prefix/run.bat", arrayOf(
			"java-runtime/bin/javaw.exe -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar resources/mardek.jar"
		))
	}

	if (os == MardekOS.Linux) {
		includeExtractedZipFile(zipOutput, gameLinuxRuntime, "$prefix/java-runtime/")
		writeTextFile(zipOutput, "$prefix/run", arrayOf(
			"#!/usr/bin/env bash",
			"./java-runtime/bin/java -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar ./resources/mardek.jar"
		), makeExecutable = true)
	}

	val tempContentFile = Files.createTempFile("content", ".bits").toFile()
	tempContentFile.deleteOnExit()
	val tempVk2dFile = Files.createTempFile("", ".vk2d").toFile()
	tempVk2dFile.deleteOnExit()

	exportTitleScreen(
		dataFile = tempContentFile,
		vk2dFile = tempVk2dFile,
		wrapped = contentWrapper,
	)

	includeFile(zipOutput, tempContentFile, "$prefix/resources/title-screen.bits")
	includeFile(zipOutput, tempVk2dFile, "$prefix/resources/title-screen.vk2d")

	exportAndPrepareVk2d(
		dataFile = tempContentFile,
		vk2dFile = tempVk2dFile,
		cacheDirectory = null,
		target = contentWrapper.content,
		compressionLevel = 22,
	)

	includeFile(zipOutput, tempContentFile, "$prefix/resources/content.bits")
	includeFile(zipOutput, tempVk2dFile, "$prefix/resources/content.vk2d")

	exportIcons(contentWrapper, Files.newOutputStream(tempContentFile.toPath()), true)
	includeFile(zipOutput, tempContentFile, "$prefix/resources/icons.bin")

	zipOutput.flush()
	zipOutput.close()
	println("Finished generating ${outputFile.absolutePath}")
}
