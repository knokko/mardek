package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.StateRenderer

class TitleScreenRenderer(boiler: BoilerInstance, private val sharedUI: SharedUiResources): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val renderer = sharedUI.uiRenderers[frameIndex]
		renderer.begin(recorder, targetImage)
		renderer.drawImage(sharedUI.bc1Images[0], 0, 0, targetImage.width, targetImage.height)
		renderer.end()
	}
}
