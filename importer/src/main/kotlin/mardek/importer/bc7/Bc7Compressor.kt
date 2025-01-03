package mardek.importer.bc7

import mardek.importer.ui.BcPacker
import org.lwjgl.system.Platform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

// TODO Test on Windows
private fun copyToTempDirectory(): File {
	val compressionDirectory = Files.createTempDirectory("").toFile()
	compressionDirectory.deleteOnExit()

	val cl = BcPacker::class.java.classLoader

	val fileNames = when (Platform.get()) {
		Platform.WINDOWS -> arrayOf("bc7enc.exe")
		Platform.LINUX -> arrayOf("bc7enc-linux")
		else -> throw UnsupportedOperationException("Unsupported OS ${Platform.get()}")
	}

	val resourcePrefix = "mardek/importer/bc7"
	for (name in fileNames) {
		val resourcePath = "$resourcePrefix/$name"
		val destination = File("$compressionDirectory/$name")
		val input = cl.getResourceAsStream(resourcePath) ?: throw Error("Can't find resource $resourcePath")
		Files.copy(input, destination.toPath())
		input.close()
	}

	if (Platform.get() == Platform.LINUX) {
		val resourcePath = "$resourcePrefix/ispc-linux.zip"
		val destination = File("$compressionDirectory/ispc")
		val input = ZipInputStream(cl.getResourceAsStream(resourcePath) ?: throw Error("Can't find resource $resourcePath"))
		val output = Files.newOutputStream(destination.toPath())

		val entry = input.nextEntry!!
		if (entry.name != "ispc") throw Error("Unexpected entry $entry")
		output.write(input.readAllBytes())
		output.flush()
		output.close()
		input.close()
	}
	for (file in compressionDirectory.listFiles()!!) {
		file.setExecutable(true)
		file.deleteOnExit()
	}
	return compressionDirectory
}

private val compressionDirectory = copyToTempDirectory()

fun compressBc7(image: BufferedImage): ByteArray {
	val name = UUID.randomUUID().toString()
	val source = File("$compressionDirectory/$name.png")
	ImageIO.write(image, "PNG", source)

	val executableName = when (Platform.get()) {
		Platform.WINDOWS -> "./bc7enc.exe"
		Platform.LINUX -> "./bc7enc-linux"
		else -> throw UnsupportedOperationException("Unsupported OS ${Platform.get()}")
	}

	val builder = ProcessBuilder(executableName, "./$name.png", "-g", "-q")
	builder.directory(compressionDirectory)
	val compressionProcess = builder.start()
	val resultCode = compressionProcess.waitFor()

	if (resultCode != 0) {
		val errorScanner = compressionProcess.errorReader()
		throw IOException("Bc7 compression failed: ${errorScanner.readLines()}")
	}

	val destination = File("$compressionDirectory/$name.dds")
	val input = Files.newInputStream(destination.toPath())
	input.skipNBytes(148)
	val result = input.readAllBytes()
	input.close()

	source.delete()
	destination.delete()

	return result
}
