package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.VkbWindow
import mardek.renderer.GameRenderer
import mardek.state.ExitState
import mardek.state.GameStateManager
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR

class GameWindow(
	window: VkbWindow,
	private val state: GameStateManager,
	private val renderer: GameRenderer
): SimpleWindowRenderLoop(
	// TODO Add support for frames in flight after fixing (tile) renderer
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
		synchronized(state.lock()) {
			renderer.render(state.currentState, recorder, acquiredImage.image(), window.surfaceFormat)
		}
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		renderer.destroy()
		synchronized(state.lock()) {
			state.currentState = ExitState()
		}
	}
}
