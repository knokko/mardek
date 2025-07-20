package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.vk2d.resource.Vk2dImageCompression
import com.github.knokko.vk2d.resource.Vk2dResourceWriter
import mardek.content.Content
import mardek.content.animations.SkeletonPartSkins
import mardek.content.sprite.BcSprite
import mardek.content.ui.Font
import mardek.content.ui.TitleScreenContent
import mardek.importer.area.SpritesAndAreas
import mardek.importer.ui.BcPacker
import mardek.importer.util.projectFolder
import java.awt.image.BufferedImage
import java.io.BufferedOutputStream
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

		saveIcon(outputFolder)

		val resourceWriter = Vk2dResourceWriter()
		val titleScreenContent = TitleScreenContent(
			background = content.ui.titleScreenBackground,
			title = content.ui.titleScreenTitle,
			smallFont = content.fonts.basic,
			largeFont = content.fonts.basicLarge,
		)

		fun addBcImage(bc: BcSprite) {
			bc.index = resourceWriter.addImage(
				bc.bufferedImage as BufferedImage, Vk2dImageCompression.BC7, false
			)
			bc.data = null
		}

		fun addFont(font: Font) {
			font.index = resourceWriter.addFont(ByteArrayInputStream(font.data))
			font.data = null
		}

		addBcImage(titleScreenContent.background)
		addBcImage(titleScreenContent.title)
		addFont(titleScreenContent.smallFont)
		addFont(titleScreenContent.largeFont)

		val output = Files.newOutputStream(File(
			"$projectFolder/game2d/src/main/resources/mardek/game/title-screen.vk2d"
		).toPath())
		resourceWriter.write(output)
		output.close()

		Files.write(
			File("$projectFolder/game2d/src/main/resources/mardek/game/title-screen.bits").toPath(),
			bitser.serializeToBytes(titleScreenContent, Bitser.BACKWARD_COMPATIBLE)
		)

//		val kimOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/kim-sprites.bin").toPath()))
		val spritesAndAreas = SpritesAndAreas()
//		for (creatureType in content.stats.creatureTypes) spritesAndAreas.registerSprite(creatureType.icon)
//		for (element in content.stats.elements) spritesAndAreas.registerSprite(element.sprite)
//		for (effect in content.stats.statusEffects) spritesAndAreas.registerSprite(effect.icon)
//		for (skillClass in content.skills.classes) spritesAndAreas.registerSprite(skillClass.icon)
//		for (item in content.items.items) spritesAndAreas.registerSprite(item.sprite)
//		for (item in content.items.plotItems) spritesAndAreas.registerSprite(item.sprite)
//		for (chestSprite in content.areas.chestSprites) {
//			spritesAndAreas.registerSprite(chestSprite.baseSprite)
//			spritesAndAreas.registerSprite(chestSprite.openedSprite)
//		}
//		for (sprite in content.ui.allKimSprites()) spritesAndAreas.registerSprite(sprite)
		spritesAndAreas.register(content.areas)
//		spritesAndAreas.writeKimSprites(kimOutput)
//		kimOutput.flush()
//		kimOutput.close()
//
//		val bcPacker = BcPacker()
//		for (element in content.stats.elements) {
//			val swingSprite = element.swingEffect
//			if (swingSprite != null) bcPacker.add(swingSprite)
//			bcPacker.add(element.bcSprite)
//			val castSprite = element.spellCastBackground
//			if (castSprite != null) bcPacker.add(castSprite)
//		}
//		for (effect in content.stats.statusEffects) {
//			for (sprite in effect.passiveParticleSprites) bcPacker.add(sprite)
//		}
//		for (sprite in content.ui.allBcSprites()) bcPacker.add(sprite)
//		for (background in content.battle.backgrounds) bcPacker.add(background.sprite)
//		for (sprite in content.battle.particleSprites) bcPacker.add(sprite.sprite)
//		for (skeleton in content.battle.skeletons) {
//			for (part in skeleton.parts) {
//				val content = part.content
//				if (content is SkeletonPartSkins) {
//					for (skin in content.skins) {
//						for (entry in skin.entries) bcPacker.add(entry.sprite)
//					}
//				}
//			}
//		}
//
//		val startTime = System.nanoTime()
//		bcPacker.compressImages()
//		println("BC took ${(System.nanoTime() - startTime) / 1000_000} ms")
//		val bcOutput = DeflaterOutputStream(Files.newOutputStream(File("$outputFolder/bc-sprites.bin").toPath()))
//		bcPacker.writeData(bcOutput)
//		bcOutput.finish()
//		bcOutput.flush()
//		bcOutput.close()
//
		val areasOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/area-offsets.bin").toPath()))
		spritesAndAreas.writeAreaOffsets(areasOutput, bitser)
		areasOutput.flush()
		areasOutput.close()

		println("exporting campaign...")
		exportCampaignData(content, outputFolder, bitser)
		println("exported campaign")
		succeeded = true
	} catch (failure: Throwable) {
		failure.printStackTrace()
	} finally {
		// For some reason, the process is not terminated by default
		exitProcess(if (succeeded) 0 else -1)
	}
}

private fun exportCampaignData(content: Content, outputFolder: File, bitser: Bitser) {
	for (sprite in content.ui.allBcSprites()) {
		sprite.data = null
	}
	for (sprite in content.ui.allKimSprites()) {
		sprite.data = null
	}
	for (background  in content.battle.backgrounds) {
		background.sprite.data = null
	}
	for (monster in content.battle.monsters) {
		for (part in monster.model.skeleton.parts) {
			if (part.content is SkeletonPartSkins) {
				for (skin in (part.content as SkeletonPartSkins).skins) {
					for (entry in skin.entries) entry.sprite.data = null
				}
			}
		}
	}
	val output = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/content.bin").toPath())))
	bitser.serialize(content, output, Bitser.BACKWARD_COMPATIBLE)
	output.finish()
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
