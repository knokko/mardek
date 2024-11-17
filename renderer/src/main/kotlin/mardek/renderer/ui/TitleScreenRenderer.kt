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

		val titleColor = srgbToLinear(rgb(160, 80, 45))
		val outline1 = srgbToLinear(rgb(68, 51, 34))
		val outline2 = srgbToLinear(rgb(190, 144, 95))
		val titleOutline = intArrayOf(titleColor, outline1, outline1, outline1, outline2, outline2, outline2)
		renderer.drawString(
			sharedUI.font, "MARDEK", titleColor, titleOutline,
			100, 100, 800, 300, 200, 100
		)
		renderer.drawString(
			sharedUI.font, "Revival project", srgbToLinear(rgb(242, 183, 113)), IntArray(0),
			110, 220, 790, 350, 320, 70
		)
		renderer.end()
	}
}
