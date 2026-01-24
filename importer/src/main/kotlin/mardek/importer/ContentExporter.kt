package mardek.importer

import com.github.knokko.bitser.Bitser
import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.vk2d.resource.Vk2dGreyscaleChannel
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.Font
import mardek.content.ui.TitleScreenContent
import mardek.importer.util.classLoader
import mardek.importer.util.projectFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
	if (!File("$projectFolder/flash/all-shapes-x4").exists()) {
		throw RuntimeException("You need to run mardek.importer.converter.SvgShapeConverter first")
	}

	var succeeded = false
	try {
		val bitser = Bitser(false)
		val content = importVanillaContent(bitser)

		val outputFolder = File("$projectFolder/game/src/main/resources/mardek/game/")
		saveIcons(outputFolder)

		saveTitleScreenBundle(bitser, content)

		println("exporting campaign...")
		saveMainContent(bitser, content, outputFolder)
		println("exported campaign")

		succeeded = true
	} catch (failure: Throwable) {
		failure.printStackTrace()
	} finally {
		// For some reason, the process is not terminated by default
		exitProcess(if (succeeded) 0 else -1)
	}
}

private fun saveIcons(outputFolder: File) {
	val iconOutput = Files.newOutputStream(File("$outputFolder/icons.bin").toPath())
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
	bc.data = null
}

private fun addKimImage(resourceWriter: Vk2dResourceWriter, kim: KimSprite) {
	kim.index = resourceWriter.addFakeImage(kim.width, kim.height, kim.data)
	kim.data = null
}

private fun addFont(resourceWriter: Vk2dResourceWriter, font: Font) {
	font.index = resourceWriter.addFont(ByteArrayInputStream(font.data))
	font.data = null
}

private fun saveMainContent(bitser: Bitser, content: Content, outputFolder: File) {
	val resourceWriter = Vk2dResourceWriter()

	val allKimSprites = ArrayList<KimSprite>()
	val allBcSprites = ArrayList<BcSprite>()
	val collectionMapping = HashMap<Class<*>, Collection<Any>>()
	collectionMapping[KimSprite::class.java] = allKimSprites
	collectionMapping[BcSprite::class.java] = allBcSprites
	bitser.collectInstances(content, collectionMapping)

	for (sprite in allKimSprites) addKimImage(resourceWriter, sprite)
	for (sprite in allBcSprites) addBcImage(resourceWriter, sprite)
	for (font in content.fonts.all()) addFont(resourceWriter, font)

	val output = Files.newOutputStream(File("$outputFolder/content.vk2d").toPath())
	resourceWriter.write(output, File("$projectFolder/flash/bc-cache"))
	output.close()

	Files.write(
		File("$outputFolder/content.bits").toPath(),
		bitser.toBytes(content, Bitser.BACKWARD_COMPATIBLE)
	)
}

private fun saveTitleScreenBundle(bitser: Bitser, content: Content) {
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
	addFont(resourceWriter, titleScreenContent.largeFont)

	val output = Files.newOutputStream(File(
		"$projectFolder/game/src/main/resources/mardek/game/title-screen.vk2d"
	).toPath())
	resourceWriter.write(output, File("$projectFolder/flash/bc-cache"))
	output.close()

	Files.write(
		File("$projectFolder/game/src/main/resources/mardek/game/title-screen.bits").toPath(),
		bitser.toBytes(titleScreenContent, Bitser.BACKWARD_COMPATIBLE)
	)
}
