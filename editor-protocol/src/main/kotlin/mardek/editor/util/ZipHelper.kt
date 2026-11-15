package mardek.editor.util

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files

fun includeFile(zipOutput: ZipArchiveOutputStream, source: File, path: String) {
	zipOutput.putArchiveEntry(ZipArchiveEntry(path))
	zipOutput.write(source.readBytes())
	zipOutput.closeArchiveEntry()
}

fun includeDirectory(zipOutput: ZipArchiveOutputStream, source: File, path: String) {
	val sourceFiles = source.listFiles() ?: throw IllegalArgumentException("${source.absolutePath} is not a directory")

	for (file in sourceFiles) {
		includeFile(zipOutput, file, path + file.name)
	}
}

fun includeExtractedZipFile(zipOutput: ZipArchiveOutputStream, source: File, path: String) {
	val input = ZipArchiveInputStream(BufferedInputStream(Files.newInputStream(source.toPath())))
	while (true) {
		val inputEntry = input.nextEntry ?: break
		val outputEntry = ZipArchiveEntry(path + inputEntry.name)
		if (inputEntry.name.contains("bin/java") || inputEntry.name.contains("lib/jspawnhelper")) {
			outputEntry.unixMode = 0x8000 or 0x1ED
		}

		zipOutput.putArchiveEntry(outputEntry)
		zipOutput.write(input.readBytes())
		zipOutput.closeArchiveEntry()
	}
	input.close()
}

fun writeTextFile(zipOutput: ZipArchiveOutputStream, path: String, lines: Array<String>, makeExecutable: Boolean = false) {
	val entry = ZipArchiveEntry(path)
	if (makeExecutable) entry.unixMode = 0x8000 or 0x1ED
	zipOutput.putArchiveEntry(entry)

	val writer = PrintWriter(zipOutput)
	for (line in lines) writer.println(line)
	writer.flush()
	zipOutput.closeArchiveEntry()
}
