package mardek.importer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.vk2d.resource.Vk2dGreyscaleChannel
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.Font
import mardek.content.ui.TitleScreenContent
import mardek.importer.ui.BcPacker
import mardek.importer.util.projectFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
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

	val itemSheet = ImageIO.read(BcPacker::class.java.classLoader.getResource(
		"mardek/importer/inventory/itemsheet_misc.png"
	))
	ImageCoding.encodeBufferedImage(imageData, itemSheet.getSubimage(288, 32, 16, 16))
	iconOutput.write(imageData.array())

	for (cursor in arrayOf("inventory", "pointer", "grab")) {
		val image = ImageIO.read(BcPacker::class.java.classLoader.getResource(
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

	for (sheet in content.areas.tilesheets) {
		for (tile in sheet.tiles) {
			for (sprite in tile.sprites) addKimImage(resourceWriter, sprite)
		}
		for (sprite in sheet.waterSprites) addKimImage(resourceWriter, sprite)
	}
	for (switchColor in content.areas.switchColors) {
		addKimImage(resourceWriter, switchColor.onSprite)
		addKimImage(resourceWriter, switchColor.offSprite)
		addKimImage(resourceWriter, switchColor.gateSprite)
		addKimImage(resourceWriter, switchColor.platformSprite)
	}
	for (characterSprite in content.areas.characterSprites) {
		for (sprite in characterSprite.sprites) addKimImage(resourceWriter, sprite)
	}
	for (objectSprite in content.areas.objectSprites) {
		for (frame in objectSprite.frames) addKimImage(resourceWriter, frame)
	}
	for (arrowSprite in content.areas.arrowSprites) addKimImage(resourceWriter, arrowSprite.sprite)
	for (chestSprite in content.areas.chestSprites) {
		addKimImage(resourceWriter, chestSprite.baseSprite)
		addKimImage(resourceWriter, chestSprite.openedSprite)
	}
	for (creatureType in content.stats.creatureTypes) addKimImage(resourceWriter, creatureType.icon)
	for (skillClass in content.skills.classes) addKimImage(resourceWriter, skillClass.icon)
	for (item in content.items.items) addKimImage(resourceWriter, item.sprite)
	for (item in content.items.plotItems) addKimImage(resourceWriter, item.sprite)
	for (sprite in content.ui.allKimSprites()) addKimImage(resourceWriter, sprite)

	for (element in content.stats.elements) {
		val swingSprite = element.swingEffect
		if (swingSprite != null) addBcImage(resourceWriter, swingSprite)
		addBcImage(resourceWriter, element.thickSprite)
		addBcImage(resourceWriter, element.thinSprite)
		val castSprite = element.spellCastBackground
		if (castSprite != null) addBcImage(resourceWriter, castSprite)
	}
	for (effect in content.stats.statusEffects) {
		addBcImage(resourceWriter, effect.icon)
		for (sprite in effect.passiveParticleSprites) addBcImage(resourceWriter, sprite)
	}
	for (sprite in content.ui.allBcSprites()) addBcImage(resourceWriter, sprite)

	for (animationSprite in content.battle.animationSprites + content.portraits.animationSprites) {
		addBcImage(resourceWriter, animationSprite.image)
	}
	for (skeleton in content.battle.skeletons) {
		for (animation in skeleton.animations.values) {
			for (sprite in animation.innerSprites) addBcImage(resourceWriter, sprite.image)
		}
	}
	addBcImage(resourceWriter, content.battle.noMask)

	for (sprite in content.battle.particleSprites) addBcImage(resourceWriter, sprite.sprite)

	for (font in content.fonts.all()) addFont(resourceWriter, font)

	val output = Files.newOutputStream(File("$outputFolder/content.vk2d").toPath())
	resourceWriter.write(output, File("$projectFolder/flash/bc-cache"))
	output.close()

	Files.write(
		File("$outputFolder/content.bits").toPath(),
		bitser.serializeToBytes(content, Bitser.BACKWARD_COMPATIBLE)
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
		bitser.serializeToBytes(titleScreenContent, Bitser.BACKWARD_COMPATIBLE)
	)
}
