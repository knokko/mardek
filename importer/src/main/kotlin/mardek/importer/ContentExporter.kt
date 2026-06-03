package mardek.importer

import com.github.knokko.bitser.options.WithParameter
import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.vk2d.resource.Vk2dGreyscaleChannel
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.Font
import mardek.content.ui.TitleScreenContent
import mardek.importer.util.classLoader
import mardek.importer.util.projectFolder
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.Zstd.ZSTD_compress
import org.lwjgl.util.zstd.Zstd.ZSTD_compressBound
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.Math.toIntExact
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
	if (!File("$projectFolder/flash/all-shapes-x4").exists()) {
		throw RuntimeException("You need to run mardek.importer.converter.SvgShapeConverter first")
	}

	var succeeded = false
	try {
		val content = importVanillaContent()

		saveIcons()

		saveTitleScreenBundle(content)

		println("exporting campaign...")
		saveMainContent(content)
		println("exported campaign")

		succeeded = true
	} catch (failure: Throwable) {
		failure.printStackTrace()
	} finally {
		// For some reason, the process is not terminated by default
		exitProcess(if (succeeded) 0 else -1)
	}
}

private fun saveIcons() {
	val iconOutput = Files.newOutputStream(File("${Content.RESOURCES_DIRECTORY}/icons.bin").toPath())
	val imageData = ByteBuffer.allocate(4 * 16 * 16)

	val itemSheet = ImageIO.read(classLoader.getResource(
		"mardek/importer/inventory/itemsheet_misc.png"
	))
	ImageCoding.encodeBufferedImage(imageData, itemSheet.getSubimage(288, 32, 16, 16))
	iconOutput.write(imageData.array())

	for (cursor in arrayOf("inventory", "pointer", "grab")) {
		val image = ImageIO.read(classLoader.getResource(
			"mardek/importer/cursors/$cursor.png"
		))
		imageData.position(0)
		ImageCoding.encodeBufferedImage(imageData, image)
		iconOutput.write(imageData.array())
	}

	iconOutput.flush()
	iconOutput.close()
}

private fun addBcImage(resourceWriter: Vk2dResourceWriter, bc: BcSprite) {
	if (bc.index != -1) return
	val compression = when (bc.version) {
		0 -> Vk2dImageCompression.NONE
		1 -> Vk2dImageCompression.BC1
		4 -> Vk2dImageCompression.BC4
		7 -> Vk2dImageCompression.BC7
		else -> throw UnsupportedOperationException("Unexpected compression BC${bc.version}")
	}
	if (bc.bufferedImage != null) {
		if (compression == Vk2dImageCompression.BC4) {
			bc.index = resourceWriter.addGreyscaleImage(
				bc.bufferedImage as BufferedImage, compression, Vk2dGreyscaleChannel.ALPHA, false
			)
		} else {
			bc.index = resourceWriter.addImage(
				bc.bufferedImage as BufferedImage, compression, false
			)
		}
	} else {
		bc.index = resourceWriter.addPreCompressedImage(
			bc.data, bc.width, bc.height,
			compression, false
		)
	}
}

private fun addKimImage(resourceWriter: Vk2dResourceWriter, kim: KimSprite) {
	kim.index = resourceWriter.addFakeImage(kim.width, kim.height, kim.data)
}

private fun addFont(resourceWriter: Vk2dResourceWriter, font: Font, extraAtlases: () -> Unit = {}) {
	val fontDataIndex = resourceWriter.addFontBlob(ByteArrayInputStream(font.data))
	font.index = resourceWriter.addFont(fontDataIndex, 0)
	resourceWriter.addAtlas(
		font.index, 8, 10f, 0.3f,
		0f, 12f, 0f,
		Float.MAX_VALUE, null,
	)
	resourceWriter.addAtlas(
		font.index, 8, 15f, 0.3f,
		0f, 20f, 0f,
		Float.MAX_VALUE, null,
	)
	extraAtlases()
	resourceWriter.addFallbackAtlas(font.index, 8, 30f, 0.3f)
}

private fun saveMainContent(content: Content) {
	val resourceWriter = Vk2dResourceWriter()

	val allKimSprites = ArrayList<KimSprite>()
	val allBcSprites = ArrayList<BcSprite>()
	val collectionMapping = HashMap<Class<*>, Collection<Any>>()
	collectionMapping[KimSprite::class.java] = allKimSprites
	collectionMapping[BcSprite::class.java] = allBcSprites
	BITSER.collectInstances(content, collectionMapping, hashMapOf())

	for (sprite in allKimSprites) addKimImage(resourceWriter, sprite)
	for (sprite in allBcSprites) addBcImage(resourceWriter, sprite)
	for (font in content.fonts.all()) addFont(resourceWriter, font)

	val output = Files.newOutputStream(File("${Content.RESOURCES_DIRECTORY}/content.vk2d").toPath())
	resourceWriter.write(output, File("$projectFolder/flash/bc-cache"))
	output.close()

	val uncompressedBytes = BITSER.toBytes(
		content, WithParameter("exporting", null)
	)
	val uncompressedBuffer = memCalloc(uncompressedBytes.size)
	uncompressedBuffer.put(0, uncompressedBytes)

	val compressedCapacity = ZSTD_compressBound(uncompressedBytes.size.toLong())
	val compressedBuffer = memCalloc(toIntExact(compressedCapacity))
	val compressedSize = ZSTD_compress(compressedBuffer, uncompressedBuffer, 22)
	compressedBuffer.limit(toIntExact(compressedSize))
	FileChannel.open(
		File("${Content.RESOURCES_DIRECTORY}/content.bits").toPath(),
		StandardOpenOption.CREATE,
		StandardOpenOption.TRUNCATE_EXISTING,
		StandardOpenOption.WRITE
	).use { channel ->
		while (compressedBuffer.position() < compressedBuffer.limit()) channel.write(compressedBuffer)
	}
	memFree(uncompressedBuffer)
	memFree(compressedBuffer)
}

private fun saveTitleScreenBundle(content: Content) {
	val resourceWriter = Vk2dResourceWriter()
	val titleScreenContent = TitleScreenContent(
		background = content.ui.titleScreenBackground,
		basicFont = content.fonts.basic2.copy(),
		fatFont = content.fonts.fat.copy(),
		largeFont = content.fonts.large2.copy(),
	)

	addBcImage(resourceWriter, titleScreenContent.background)
	addFont(resourceWriter, titleScreenContent.basicFont)
	addFont(resourceWriter, titleScreenContent.fatFont)
	addFont(resourceWriter, titleScreenContent.largeFont) {
		resourceWriter.addAtlas(
			titleScreenContent.largeFont.index,
			8, 200f, 0.12f,
			0f, Float.MAX_VALUE,
			0f, 0.12f, "MARDEK"
		)
	}

	val output = Files.newOutputStream(File("${Content.RESOURCES_DIRECTORY}/title-screen.vk2d").toPath())
	resourceWriter.write(output, File("$projectFolder/flash/bc-cache"))
	output.close()

	Files.write(
		File("${Content.RESOURCES_DIRECTORY}/title-screen.bits").toPath(),
		BITSER.toBytes(
			titleScreenContent, WithParameter("exporting", null),
		)
	)
}
