package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.SwapchainResourceManager
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.profiler.SampleProfiler
import com.github.knokko.profiler.storage.SampleStorage
import com.github.knokko.update.UpdateCounter
import mardek.content.Content
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.ExitState
import mardek.state.GameStateManager
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED
import org.lwjgl.sdl.SDLEvents.SDL_PushEvent
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowID
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.sdl.SDL_WindowEvent
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import java.util.concurrent.CompletableFuture

class GameWindow(
	window: VkbWindow,
	framesInFlight: Int,
	private val getResources: CompletableFuture<SharedResources>,
	private val getContent: CompletableFuture<Content>,
	private val state: GameStateManager,
	private val mainStartTime: Long
): SimpleWindowRenderLoop(
	window, framesInFlight, false,
	window.supportedPresentModes.find { it == VK_PRESENT_MODE_FIFO_KHR } ?: VK_PRESENT_MODE_IMMEDIATE_KHR,
	ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
) {
	private lateinit var renderer: GameRenderer
	private lateinit var framebuffers: SwapchainResourceManager<Any, Long>

	private val updateCounter = UpdateCounter()
	private var lastFps = -1L
	private val profileStorage = SampleStorage.frequency()
	private val profiler = SampleProfiler(profileStorage)

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)
		renderer = GameRenderer(getResources)
		framebuffers = SwapchainResources(boiler, getResources)
	}
//	init {
//		profiler.start()
//	}

	private var firstFrame = true

	override fun recordFrame(
		stack: MemoryStack,
		frameIndex: Int,
		recorder: CommandRecorder,
		acquiredImage: AcquiredImage,
		instance: BoilerInstance
	) {
		val startTime = System.nanoTime()
		val currentFps = updateCounter.value
		if (currentFps != lastFps) {
			println("FPS is $currentFps")
			lastFps = currentFps
		}
		updateCounter.increment()

		val framebuffer = framebuffers.get(acquiredImage)
		synchronized(state.lock()) {
			if (state.currentState is ExitState) {
				val quitEvent = SDL_Event.calloc(stack)
				quitEvent.type(SDL_EVENT_WINDOW_CLOSE_REQUESTED)
				SDL_WindowEvent.nwindowID(quitEvent.address(), SDL_GetWindowID(window.handle))
				assertSdlSuccess(SDL_PushEvent(quitEvent), "PushEvent")
			}
			else renderer.render(
				getContent, state, recorder, acquiredImage.image(),
				framebuffer, frameIndex, state.soundQueue
			)
		}

		if (firstFrame) {
			firstFrame = false
			println("first frame took ${(System.nanoTime() - startTime) / 1000_000} ms and was submitted after ${(System.nanoTime() - mainStartTime) / 1000_000} ms")
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
