package mardek.miscellaneous

import java.io.File

fun main() {
	val baseDirectory = File("../game/rendering-test-results")
	val expectedDirectory = File("$baseDirectory/expected")
	val actualDirectory = File("$baseDirectory/actual")

	val expectedFilesArray = expectedDirectory.list() ?:
			throw RuntimeException("Couldn't find expected screenshots")
	val actualFilesArray = actualDirectory.list() ?:
			throw RuntimeException("Couldn't find actual screenshots. Did you even run the tests?")

	val expectedFiles = expectedFilesArray.toSet()
	val actualFiles = actualFilesArray.toSet()
	if (expectedFiles.size < 10) throw RuntimeException("Too few expected files: $expectedFiles")

	val missingFiles = actualFiles - expectedFiles
	if (missingFiles.isNotEmpty()) throw RuntimeException("Missing screenshots $missingFiles")

	val unexpectedFiles = expectedFiles - actualFiles
	if (unexpectedFiles.isNotEmpty()) throw RuntimeException("Unexpected screenshots $unexpectedFiles")
}
