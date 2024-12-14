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
import java.nio.file.Files

fun main() {
	val bitser = Bitser(false)
	val campaign = importDefaultCampaign(bitser)

	val outputFolder = File("game/src/main/resources/mardek/game/")
	exportAreaAssets(campaign, outputFolder)
	exportUiAssets(campaign, outputFolder)
	exportCampaignData(campaign, outputFolder, bitser)
}

private fun exportCampaignData(campaign: Campaign, outputFolder: File, bitser: Bitser) {
	val output = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/campaign.bin").toPath())))
	bitser.serialize(campaign, output)
	output.finish()
}

private fun exportAreaAssets(campaign: Campaign, outputFolder: File) {
	val areaSprites = AreaSprites()
	areaSprites.register(campaign.areas)

	val renderOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/area-assets.bin").toPath()))
	areaSprites.writeRenderData(renderOutput)
	renderOutput.close()
}

private fun exportUiAssets(campaign: Campaign, outputFolder: File) {
	val packer = UiPacker()
	packer.addBc1("TitleScreenBackground.png")
	for (item in campaign.inventory.items) packer.addKim1(item.sprite)

	val boiler = BoilerBuilder(
		VK_API_VERSION_1_0, "ExportUiResources", 1
	).validation().forbidValidationErrors().build()

	val output = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/ui-assets.bin").toPath()))
	packer.writeDataAndDestroy(boiler, output)
	output.flush()
	output.close()

	boiler.destroyInitialObjects()
}
