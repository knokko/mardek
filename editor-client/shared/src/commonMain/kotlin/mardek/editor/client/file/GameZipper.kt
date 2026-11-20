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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun zipGame(contentWrapper: ContentWrapper, outputFile: File, os: MardekOS, reportProgress: (String) -> Unit) {
	println("Zipping game executable to ${outputFile.absolutePath}")

	val gameJar = File("${Content.RESOURCES_DIRECTORY}/../game/build/libs/mardek.jar")
	val musicFolder = File("${Content.RESOURCES_DIRECTORY}/music")
	val gameWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-windows-x64.zip")
	val gameLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-linux-x64.zip")

	val zipOutput = ZipArchiveOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	val prefix = "/mardek-$os"
	run {
		zipOutput.putArchiveEntry(ZipArchiveEntry("$prefix/resources/mardek.jar"))
		val gameInput = ZipInputStream(BufferedInputStream(FileInputStream(gameJar)))
		val gameOutput = ZipOutputStream(zipOutput)

		while (true) {
			val gameEntry = gameInput.nextEntry ?: break

			val osPrefixes = arrayOf("windows", "linux", "macos", "freebsd")
			if (osPrefixes.any { gameEntry.name.startsWith("$it/") }) {
				if (os == MardekOS.Windows && !gameEntry.name.startsWith("windows/x64/")) continue
				if (os == MardekOS.Linux && !gameEntry.name.startsWith("linux/x64/")) continue
			}

			gameOutput.putNextEntry(gameEntry)
			gameOutput.write(gameInput.readBytes())
			gameOutput.closeEntry()
		}
		gameOutput.finish()
		zipOutput.closeArchiveEntry()
	}

	val numSteps = 4
	reportProgress("step 2 / $numSteps: bundling music")
	includeDirectory(zipOutput, musicFolder, "$prefix/resources/music/")

	reportProgress("step 3 / $numSteps: bundling java runtime")
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

	reportProgress("step 4 / $numSteps: exporting content")
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
	reportProgress("")
}
