package mardek.importer.ui

import com.github.knokko.boiler.builders.BoilerBuilder
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0
import java.io.File
import java.nio.file.Files

class TestUiPacker {

	@Test
	fun exportUiResources() {
		val packer = UiPacker()
		packer.addBc1("TitleScreenBackground.png")

		val boiler = BoilerBuilder(
			VK_API_VERSION_1_0, "ExportUiResources", 1
		).validation().forbidValidationErrors().build()

		val outputFolder = File("../game/src/main/resources/mardek/game/")
		val outputFile = File("$outputFolder/ui-assets.bin")
		val output = Files.newOutputStream(outputFile.toPath())
		packer.writeDataAndDestroy(boiler, output)
		output.flush()
		output.close()

		boiler.destroyInitialObjects()
	}
}
