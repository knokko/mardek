package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.area.AreaRenderer
import mardek.renderer.area.SharedAreaResources
import mardek.state.ingame.GameProgression

class InGameRenderer(
	private val progress: GameProgression,
	boiler: BoilerInstance,
	private val resources: SharedAreaResources,
): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val area = progress.currentArea
		if (area != null) AreaRenderer(area, progress.characters, resources).render(recorder, targetImage, frameIndex)
	}
}
