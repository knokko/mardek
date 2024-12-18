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
	val renderOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/render-assets.bin").toPath()))
	exportUiAssets(campaign, renderOutput)
	exportAreaAssets(campaign, renderOutput, bitser)
	renderOutput.flush()
	renderOutput.close()
	exportCampaignData(campaign, outputFolder, bitser)
}

private fun exportCampaignData(campaign: Campaign, outputFolder: File, bitser: Bitser) {
	val output = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/campaign.bin").toPath())))
	bitser.serialize(campaign, output)
	output.finish()
}

private fun exportAreaAssets(campaign: Campaign, renderOutput: OutputStream, bitser: Bitser) {
	val areaSprites = AreaSprites()
	for (item in campaign.inventory.items) areaSprites.registerSprite(item.sprite)
	areaSprites.register(campaign.areas)
	areaSprites.writeRenderData(renderOutput, bitser)
	// TODO Unify with UI assets
}

private fun exportUiAssets(campaign: Campaign, output: OutputStream) {
	val packer = UiPacker()
	packer.addBc1("TitleScreenBackground.png")

	val boiler = BoilerBuilder(
		VK_API_VERSION_1_0, "ExportUiResources", 1
	).validation().forbidValidationErrors().build()

	packer.writeDataAndDestroy(boiler, output)
	boiler.destroyInitialObjects()
	output.flush()
}
