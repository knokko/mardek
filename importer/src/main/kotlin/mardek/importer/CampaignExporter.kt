package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.Campaign
import mardek.importer.area.AreaSprites
import mardek.importer.ui.BcPacker
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

fun main() {
	val bitser = Bitser(false)
	val campaign = importDefaultCampaign(bitser)
	val outputFolder = File("game/src/main/resources/mardek/game/")

	val kimOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/kim-sprites.bin").toPath()))
	val areaSprites = AreaSprites()
	for (element in campaign.combat.elements) areaSprites.registerSprite(element.sprite)
	for (skillClass in campaign.skills.classes) areaSprites.registerSprite(skillClass.icon)
	for (item in campaign.inventory.items) areaSprites.registerSprite(item.sprite)
	for (item in campaign.inventory.plotItems) areaSprites.registerSprite(item.sprite)
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
	println("BC took ${(System.nanoTime() - startTime) / 1000_000}")
	val bcOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/bc-sprites.bin").toPath()))
	bcPacker.writeData(bcOutput)
	bcOutput.flush()
	bcOutput.close()

	val areasOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/area-offsets.bin").toPath()))
	areaSprites.writeAreaOffsets(areasOutput, bitser)
	areasOutput.flush()
	areasOutput.close()

	exportCampaignData(campaign, outputFolder, bitser)
}

private fun exportCampaignData(campaign: Campaign, outputFolder: File, bitser: Bitser) {
	val output = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/campaign.bin").toPath())))
	bitser.serialize(campaign, output)
	output.finish()
}
