package mardek.playground

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.boiler.window.WindowEventLoop
import mardek.importer.area.importArea
import mardek.renderer.GameRenderer
import mardek.state.GameState
import mardek.state.area.AreaPosition
import mardek.state.area.AreaState
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import org.lwjgl.vulkan.VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT

fun main() {
	val area = importArea("aeropolis_N")
	val state = AreaState(area, AreaPosition(1, 2))

	val boiler = BoilerBuilder(
		VK_API_VERSION_1_2, "ImportPlayground", 2
	)
		.validation()
		.enableDynamicRendering()
		.addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
		.build()

	val renderer = GameRenderer(boiler)

	val eventLoop = WindowEventLoop()
	eventLoop.addWindow(ImportPlayground2(boiler.window(), renderer, state))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}

class ImportPlayground2(window: VkbWindow, private val renderer: GameRenderer, private val state: GameState) : SimpleWindowRenderLoop(
	window, 1, true, VK_PRESENT_MODE_MAILBOX_KHR,
	ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
) {
	override fun recordFrame(
		stack: MemoryStack,
		frameIndex: Int,
		recorder: CommandRecorder,
		acquiredImage: AcquiredImage,
		instance: BoilerInstance
	) {
		renderer.render(state, recorder, acquiredImage.image(), window.surfaceFormat)
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		renderer.destroy()
	}
}
