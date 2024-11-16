package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.profiler.SampleProfiler
import com.github.knokko.profiler.storage.SampleStorage
import com.github.knokko.update.UpdateCounter
import mardek.assets.GameAssets
import mardek.renderer.GameRenderer
import mardek.state.ExitState
import mardek.state.GameStateManager
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR

class GameWindow(
	private val assets: GameAssets,
	window: VkbWindow,
	private val state: GameStateManager,
): SimpleWindowRenderLoop(
	window, 2, false, VK_PRESENT_MODE_MAILBOX_KHR,
	ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
) {
	private lateinit var renderer: GameRenderer

	private val updateCounter = UpdateCounter()
	private var lastFps = -1L
	private val profileStorage = SampleStorage.frequency()
	private val profiler = SampleProfiler(profileStorage)

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)
		renderer = GameRenderer(
			assets, boiler, window.surfaceFormat, numFramesInFlight,
			"mardek/game/area-assets.bin", "mardek/game/ui-assets.bin"
		)
	}
//	init {
//		profiler.start()
//	}

	override fun recordFrame(
		stack: MemoryStack,
		frameIndex: Int,
		recorder: CommandRecorder,
		acquiredImage: AcquiredImage,
		instance: BoilerInstance
	) {
		val currentFps = updateCounter.value
		if (currentFps != lastFps) {
			println("FPS is $currentFps")
			lastFps = currentFps
		}
		updateCounter.increment()
		synchronized(state.lock()) {
			renderer.render(
				state.currentState, recorder, acquiredImage.image(), frameIndex
			)
		}
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		renderer.destroy()
		synchronized(state.lock()) {
			state.currentState = ExitState()
		}

//		profiler.stop()
//		profileStorage.getThreadStorage(Thread.currentThread().id).print(System.out, 15, 1.0)
	}
}
