package mardek.importer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.Content
import mardek.content.animations.SkeletonPartSkins
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
		saveTitleScreenBundle(bitser, content)

		val outputFolder = File("$projectFolder/game2d/src/main/resources/mardek/game/")
		saveIcon(outputFolder)

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

private fun saveIcon(outputFolder: File) {
	val itemSheet = ImageIO.read(BcPacker::class.java.classLoader.getResource(
		"mardek/importer/inventory/itemsheet_misc.png"
	))
	val imageData = ByteBuffer.allocate(4 * 16 * 16)
	ImageCoding.encodeBufferedImage(imageData, itemSheet.getSubimage(288, 32, 16, 16))
	val iconOutput = Files.newOutputStream(File("$outputFolder/icon.bin").toPath())
	iconOutput.write(imageData.array())
	iconOutput.flush()
	iconOutput.close()
}

private fun addBcImage(resourceWriter: Vk2dResourceWriter, bc: BcSprite) {
	if (bc.bufferedImage != null) {
		bc.index = resourceWriter.addImage(
			bc.bufferedImage as BufferedImage, Vk2dImageCompression.BC7, false
		)
	}
	// TODO Handle the else
	bc.data = null
}

private fun addKimImage(resourceWriter: Vk2dResourceWriter, kim: KimSprite) {
	if (kim.version == 3) kim.offset = resourceWriter.addFakeImage(kim.width, kim.height, kim.data)
	// TODO kim.offset = resourceWriter.addFakeImage(kim.)
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
	for (element in content.stats.elements) addKimImage(resourceWriter, element.sprite)
	for (effect in content.stats.statusEffects) addKimImage(resourceWriter, effect.icon)
	for (skillClass in content.skills.classes) addKimImage(resourceWriter, skillClass.icon)
	for (item in content.items.items) addKimImage(resourceWriter, item.sprite)
	for (item in content.items.plotItems) addKimImage(resourceWriter, item.sprite)
	for (chestSprite in content.areas.chestSprites) {
		addKimImage(resourceWriter, chestSprite.baseSprite)
		addKimImage(resourceWriter, chestSprite.openedSprite)
	}
	for (sprite in content.ui.allKimSprites()) addKimImage(resourceWriter, sprite)

	for (element in content.stats.elements) {
		val swingSprite = element.swingEffect
		if (swingSprite != null) addBcImage(resourceWriter, swingSprite)
		addBcImage(resourceWriter, element.bcSprite)
		val castSprite = element.spellCastBackground
		if (castSprite != null) addBcImage(resourceWriter, castSprite)
	}
	for (effect in content.stats.statusEffects) {
		for (sprite in effect.passiveParticleSprites) addBcImage(resourceWriter, sprite)
	}
	for (sprite in content.ui.allBcSprites()) addBcImage(resourceWriter, sprite)
	for (background in content.battle.backgrounds) addBcImage(resourceWriter, background.sprite)
	for (sprite in content.battle.particleSprites) addBcImage(resourceWriter, sprite.sprite)
	for (skeleton in content.battle.skeletons) {
		for (part in skeleton.parts) {
			val content = part.content
			if (content is SkeletonPartSkins) {
				for (skin in content.skins) {
					for (entry in skin.entries) addBcImage(resourceWriter, entry.sprite)
				}
			}
		}
	}

	val output = Files.newOutputStream(File("$outputFolder/content.vk2d").toPath())
	resourceWriter.write(output)
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
		title = content.ui.titleScreenTitle,
		smallFont = content.fonts.basic,
		largeFont = content.fonts.basicLarge,
	)

	addBcImage(resourceWriter, titleScreenContent.background)
	addBcImage(resourceWriter, titleScreenContent.title)
	addFont(resourceWriter, titleScreenContent.smallFont)
	addFont(resourceWriter, titleScreenContent.largeFont)

	val output = Files.newOutputStream(File(
		"$projectFolder/game2d/src/main/resources/mardek/game/title-screen.vk2d"
	).toPath())
	resourceWriter.write(output)
	output.close()

	Files.write(
		File("$projectFolder/game2d/src/main/resources/mardek/game/title-screen.bits").toPath(),
		bitser.serializeToBytes(titleScreenContent, Bitser.BACKWARD_COMPATIBLE)
	)
}
