package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.area.AreaRenderer
import mardek.renderer.area.SharedAreaResources
import mardek.state.InGameState
import org.lwjgl.system.MemoryStack.stackPush

class InGameRenderer(
	private val state: InGameState,
	boiler: BoilerInstance,
	private val targetImageFormat: Int,
	private val framesInFlight: Int
): StateRenderer(boiler) {

	private val areaResources = stackPush().use { stack -> SharedAreaResources(boiler, stack, targetImageFormat) }
	private var lastArea = state.area
	private var areaRenderer = createAreaRenderer()

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		if (state.area != lastArea) {
			areaRenderer.destroy()
			lastArea = state.area
			areaRenderer = createAreaRenderer()
		}
		areaRenderer.render(recorder, targetImage, frameIndex)
	}

	override fun destroy() {
		areaRenderer.destroy()
		areaResources.destroy()
	}

	private fun createAreaRenderer() = stackPush().use {
		stack -> AreaRenderer(lastArea, state.story, boiler, areaResources, stack, framesInFlight)
	}
}
