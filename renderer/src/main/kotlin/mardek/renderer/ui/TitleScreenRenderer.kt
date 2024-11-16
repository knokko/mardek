package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import mardek.renderer.StateRenderer

class TitleScreenRenderer(boiler: BoilerInstance, private val sharedUI: SharedUiResources): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val renderer = sharedUI.uiRenderers[frameIndex]
		renderer.begin(recorder, targetImage)
		renderer.drawImage(sharedUI.bc1Images[0], 0, 0, targetImage.width, targetImage.height)
		renderer.drawString(
			sharedUI.font, "MARDEK", srgbToLinear(rgb(160, 80, 45)),
			100, 100, 800, 300, 200, 100
		)
		renderer.drawString(
			sharedUI.font, "Revival project", srgbToLinear(rgb(242, 183, 113)),
			110, 220, 790, 350, 320, 70
		)
		renderer.end()
	}
}
