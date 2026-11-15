package mardek.editor.server

import mardek.content.Content
import mardek.editor.MardekOS
import mardek.editor.util.includeDirectory
import mardek.editor.util.includeExtractedZipFile
import mardek.editor.util.includeFile
import mardek.editor.util.writeTextFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID

fun zipEditorClient(outputFile: File, serverProperties: EditorServerProperties, clientPassword: UUID, os: MardekOS) {
	println("Zipping client executable to ${outputFile.absolutePath}")

	val gameJar = File("${Content.RESOURCES_DIRECTORY}/../mardek.jar")
	val clientJar = File("${Content.RESOURCES_DIRECTORY}/../editor-client.jar")
	val musicFolder = File("${Content.RESOURCES_DIRECTORY}/music")
	val gameWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-windows-x64.zip")
	val gameLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-linux-x64.zip")
	val editorWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-windows-x64.zip")
	val editorLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-linux-x64.zip")

	val zipOutput = ZipArchiveOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	includeFile(zipOutput, gameJar, "/mardek.jar")
	includeFile(zipOutput, clientJar, "/editor-client.jar")
	includeDirectory(zipOutput, musicFolder, "/resources/music/")

	includeFile(zipOutput, gameWindowsRuntime, "/resources/java-runtime-windows.zip")
	includeFile(zipOutput, gameLinuxRuntime, "/resources/java-runtime-linux.zip")

	if (os == MardekOS.Windows) {
		includeExtractedZipFile(zipOutput, editorWindowsRuntime, "/java-runtime/")
		writeTextFile(zipOutput, "/run", arrayOf(
			"java-runtime/bin/javaw.exe -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar editor-client.jar"
		))
	}

	if (os == MardekOS.Linux) {
		includeExtractedZipFile(zipOutput, editorLinuxRuntime, "/java-runtime/")
		writeTextFile(zipOutput, "/run", arrayOf(
			"#!/usr/bin/env bash",
			"./java-runtime/bin/java -XX:+UseZGC -XX:ZUncommitDelay=5 -XX:SoftMaxHeapSize=500m -jar ./editor-client.jar"
		), makeExecutable = true)
	}

	writeTextFile(zipOutput, "/editor-client/client.properties", arrayOf(
		"server-ip=${serverProperties.ip}",
		"server-port=${serverProperties.port}",
		"password=$clientPassword"
	))

	zipOutput.putArchiveEntry(ZipArchiveEntry("/resources/"))
	zipOutput.closeArchiveEntry()

	val publicKeyFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/certificate/public-certificate.pem")
	includeFile(zipOutput, publicKeyFile, "/editor-server/certificate/public-certificate.pem")

	zipOutput.flush()
	zipOutput.close()
	println("Finished generating ${outputFile.absolutePath}")
}
