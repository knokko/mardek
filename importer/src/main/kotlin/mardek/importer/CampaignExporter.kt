package mardek.importer

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.builders.BoilerBuilder
import mardek.assets.Campaign
import mardek.importer.area.AreaSprites
import mardek.importer.ui.UiPacker
import org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
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

	val bcOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/bc1-sprites.bin").toPath()))
	exportBc1Sprites(bcOutput)
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

private fun exportBc1Sprites(output: OutputStream) {
	val packer = UiPacker()
	packer.addBc1("TitleScreenBackground.png")
	packer.addBc1("TitleMARDEK.png")

	val boiler = BoilerBuilder(
		VK_API_VERSION_1_0, "ExportBc1Sprites", 1
	).validation().forbidValidationErrors().build()

	packer.writeDataAndDestroy(boiler, output)
	boiler.destroyInitialObjects()
}
