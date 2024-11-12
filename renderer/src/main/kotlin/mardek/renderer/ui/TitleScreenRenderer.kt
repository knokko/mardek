package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.text.placement.TextPlaceRequest
import mardek.renderer.StateRenderer

class TitleScreenRenderer(boiler: BoilerInstance, private val sharedUI: SharedUiResources): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val textRequests = listOf(TextPlaceRequest(
			"MARDEK", 100, 100, 800, 400, 300, 100, rgb(160, 80, 45)
		))
		sharedUI.textRenderers[frameIndex].recordCommands(recorder, targetImage.width, targetImage.height, textRequests)
	}
}
