package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.importer.area.AreaSprites
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
		val campaign = importVanillaContent(bitser)
		val outputFolder = File("$projectFolder/game/src/main/resources/mardek/game/")

		val kimOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/kim-sprites.bin").toPath()))
		val areaSprites = AreaSprites()
		for (element in campaign.stats.elements) areaSprites.registerSprite(element.sprite)
		for (skillClass in campaign.skills.classes) areaSprites.registerSprite(skillClass.icon)
		for (item in campaign.items.items) areaSprites.registerSprite(item.sprite)
		for (item in campaign.items.plotItems) areaSprites.registerSprite(item.sprite)
		for (chestSprite in campaign.areas.chestSprites) {
			areaSprites.registerSprite(chestSprite.baseSprite)
			areaSprites.registerSprite(chestSprite.openedSprite)
		}
		for (sprite in campaign.ui.allKimSprites()) areaSprites.registerSprite(sprite)
		areaSprites.register(campaign.areas)
		areaSprites.writeKimSprites(kimOutput)
		kimOutput.flush()
		kimOutput.close()

		val bcPacker = BcPacker()
		for (sprite in campaign.ui.allBcSprites()) bcPacker.add(sprite)
		for (background in campaign.battle.backgrounds) bcPacker.add(background.sprite)
		for (skeleton in campaign.battle.skeletons) {
			for (part in skeleton.parts) {
				for (skin in part.skins) {
					for (entry in skin.entries) bcPacker.add(entry.sprite)
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
		areaSprites.writeAreaOffsets(areasOutput, bitser)
		areasOutput.flush()
		areasOutput.close()

		println("exporting campaign...")
		exportCampaignData(campaign, outputFolder, bitser)
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
