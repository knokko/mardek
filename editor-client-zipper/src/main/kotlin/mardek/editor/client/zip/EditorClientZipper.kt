package mardek.editor.client.zip

import mardek.content.Content
import mardek.editor.MardekOS
import mardek.editor.util.includeDirectory
import mardek.editor.util.includeExtractedZipFile
import mardek.editor.util.includeFile
import mardek.editor.util.writeTextFile
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

fun main(args: Array<String>) {
	if (args.size != 1) {
		throw RuntimeException("You should use ./gradlew zipEditorClient --args=\"OS\"")
	}
	val rawOS = args[0]
	val os = MardekOS.getByName(rawOS)
	if (os == null) {
		throw RuntimeException("Can't find OS $os: use one of ${MardekOS.entries.map { it.name }}")
	}

	val outputFile = File("./editor-client-$os.zip")
	println("Zipping client executable to ${outputFile.absolutePath}")

	val gameJar = File("${Content.RESOURCES_DIRECTORY}/../game/build/libs/mardek.jar")
	val clientJar = File("${Content.RESOURCES_DIRECTORY}/../editor-client/desktopApp/build/libs/desktopApp-all.jar")
	val musicFolder = File("${Content.RESOURCES_DIRECTORY}/music")
	val gameWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-windows-x64.zip")
	val gameLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-linux-x64.zip")
	val editorWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-windows-x64.zip")
	val editorLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-linux-x64.zip")

	val zipOutput = ZipArchiveOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	includeFile(zipOutput, gameJar, "/mardek-editor-client/game/build/libs/mardek.jar")
	includeFile(zipOutput, clientJar, "/mardek-editor-client/resources/editor-client.jar")
	includeDirectory(zipOutput, musicFolder, "/mardek-editor-client/resources/music/")

	includeFile(zipOutput, gameWindowsRuntime, "/mardek-editor-client/resources/game-java-runtime-windows-x64.zip")
	includeFile(zipOutput, gameLinuxRuntime, "/mardek-editor-client/resources/game-java-runtime-linux-x64.zip")

	if (os == MardekOS.Windows) {
		includeExtractedZipFile(zipOutput, editorWindowsRuntime, "/mardek-editor-client/java-runtime/")
		writeTextFile(zipOutput, "/mardek-editor-client/run.bat", arrayOf(
			"java-runtime/bin/javaw.exe -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar resources/editor-client.jar"
		))
	}

	if (os == MardekOS.Linux) {
		includeExtractedZipFile(zipOutput, editorLinuxRuntime, "/mardek-editor-client/java-runtime/")
		writeTextFile(zipOutput, "/mardek-editor-client/run", arrayOf(
			"#!/usr/bin/env bash",
			"./java-runtime/bin/java -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar ./resources/editor-client.jar"
		), makeExecutable = true)
	}

	zipOutput.flush()
	zipOutput.close()
	println("Finished generating ${outputFile.absolutePath}")
}
