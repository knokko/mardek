package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.StateRenderer

class TitleScreenRenderer(boiler: BoilerInstance, private val sharedUI: SharedUiResources): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val renderer = sharedUI.uiRenderers[frameIndex]
		renderer.begin(recorder, targetImage)
		renderer.drawImage(sharedUI.bc1Images[0], 0, 0, targetImage.width, targetImage.height)

		run {
			val darkColor = srgbToLinear(rgb(90, 51, 17))
			val mediumColor = srgbToLinear(rgb(180, 90, 65))
			val lightColor = srgbToLinear(rgb(233, 194, 186))
			val outline1 = srgbToLinear(rgb(68, 51, 34))
			val outline2 = srgbToLinear(rgb(190, 144, 95))
			val titleOutline = intArrayOf(outline1, outline1, outline1, outline2, outline2, outline2)

			val minX = 100
			val minY = 80
			val baseY = 200
			val width = 700
			val height = 100

			val darkHeight = height * 3 / 10
			val lightHeight = height * 2 / 10
			val gy = baseY - minY - height + titleOutline.size
			renderer.drawString(
				sharedUI.font, "MARDEK", mediumColor, titleOutline,
				minX, minY, minX + width - 1, 300, baseY, height,
				Gradient(0, gy, width, darkHeight, mediumColor, mediumColor, darkColor),
				Gradient(0, gy + darkHeight, width, lightHeight, lightColor, lightColor, mediumColor),
				Gradient(0, gy + darkHeight + lightHeight, width, lightHeight, mediumColor, mediumColor, lightColor),
				Gradient(0, gy + darkHeight + 2 * lightHeight, width, darkHeight, darkColor, darkColor, mediumColor)
			)
		}

		run {
			val height = 70
			val minY = 220
			val baseY = 320
			val darkTint = srgbToLinear(rgb(185, 131, 60))
			renderer.drawString(
				sharedUI.font, "Revival project", srgbToLinear(rgb(242, 183, 113)), IntArray(0),
				110, minY, 790, 350, baseY, height,
				Gradient(0, baseY - minY - height / 3, 1000, height, darkTint, darkTint, darkTint)
			)
		}

		renderer.end()
	}
}
