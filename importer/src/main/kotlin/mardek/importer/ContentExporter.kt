package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.content.animations.SkeletonPartSkins
import mardek.importer.area.SpritesAndAreas
import mardek.importer.ui.BcPacker
import mardek.importer.util.projectFolder
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.DeflaterOutputStream
import kotlin.system.exitProcess

fun main() {
	var succeeded = false
	try {
		val bitser = Bitser(false)
		val content = importVanillaContent(bitser)
		val outputFolder = File("$projectFolder/game/src/main/resources/mardek/game/")

		val kimOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/kim-sprites.bin").toPath()))
		val spritesAndAreas = SpritesAndAreas()
		for (creatureType in content.stats.creatureTypes) spritesAndAreas.registerSprite(creatureType.icon)
		for (element in content.stats.elements) spritesAndAreas.registerSprite(element.sprite)
		for (effect in content.stats.statusEffects) spritesAndAreas.registerSprite(effect.icon)
		for (skillClass in content.skills.classes) spritesAndAreas.registerSprite(skillClass.icon)
		for (item in content.items.items) spritesAndAreas.registerSprite(item.sprite)
		for (item in content.items.plotItems) spritesAndAreas.registerSprite(item.sprite)
		for (chestSprite in content.areas.chestSprites) {
			spritesAndAreas.registerSprite(chestSprite.baseSprite)
			spritesAndAreas.registerSprite(chestSprite.openedSprite)
		}
		for (sprite in content.ui.allKimSprites()) spritesAndAreas.registerSprite(sprite)
		spritesAndAreas.register(content.areas)
		spritesAndAreas.writeKimSprites(kimOutput)
		kimOutput.flush()
		kimOutput.close()

		val bcPacker = BcPacker()
		for (element in content.stats.elements) {
			val swingSprite = element.swingEffect
			if (swingSprite != null) bcPacker.add(swingSprite)
			bcPacker.add(element.bcSprite)
			val castSprite = element.spellCastBackground
			if (castSprite != null) bcPacker.add(castSprite)
		}
		for (effect in content.stats.statusEffects) {
			for (sprite in effect.passiveParticleSprites) bcPacker.add(sprite)
		}
		for (sprite in content.ui.allBcSprites()) bcPacker.add(sprite)
		for (background in content.battle.backgrounds) bcPacker.add(background.sprite)
		for (sprite in content.battle.particleSprites) bcPacker.add(sprite.sprite)
		for (skeleton in content.battle.skeletons) {
			for (part in skeleton.parts) {
				val content = part.content
				if (content is SkeletonPartSkins) {
					for (skin in content.skins) {
						for (entry in skin.entries) bcPacker.add(entry.sprite)
					}
				}
			}
		}

		val startTime = System.nanoTime()
		bcPacker.compressImages()
		println("BC took ${(System.nanoTime() - startTime) / 1000_000} ms")
		val bcOutput = DeflaterOutputStream(Files.newOutputStream(File("$outputFolder/bc-sprites.bin").toPath()))
		bcPacker.writeData(bcOutput)
		bcOutput.finish()
		bcOutput.flush()
		bcOutput.close()

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
	val output = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/content.bin").toPath())))
	bitser.serialize(content, output, Bitser.BACKWARD_COMPATIBLE)
	output.finish()
}
