package mardek.game

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.update.UpdateCounter
import com.github.knokko.update.UpdateLoop
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.Vk2dWindow
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import mardek.audio.AudioUpdater
import mardek.content.Content
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderResources
import mardek.renderer.renderGame
import mardek.state.ExitState
import mardek.state.GameStateManager
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED
import org.lwjgl.sdl.SDLEvents.SDL_PushEvent
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowID
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.sdl.SDL_WindowEvent
import org.lwjgl.system.MemoryStack
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

class MardekWindow(
	private val gameState: GameStateManager, window: VkbWindow, capFps: Boolean
) : Vk2dWindow(window, capFps) {

	private lateinit var renderResources: RenderResources
	private var totalFrames = 0L

	private val fpsCounter = UpdateCounter()
	private var printFpsAt = System.nanoTime() + 1500_000_000L

	override fun initialResourceBundle(): InputStream {
		return MardekWindow::class.java.getResourceAsStream("title-screen.vk2d")!!
	}

	override fun createResources(boiler: BoilerInstance, combiner: MemoryCombiner, descriptors: DescriptorCombiner) {
		super.createResources(boiler, combiner, descriptors)
		val pipelineContext = Vk2dPipelineContext.renderPass(boiler, window.surfaceFormat)
		this.renderResources = RenderResources(
			boiler, pipelineContext, combiner, descriptors, numFramesInFlight
		)
	}

	override fun renderFrame(
		frame: Vk2dFrame,
		recorder: CommandRecorder,
		swapchainImage: AcquiredImage,
		boiler: BoilerInstance
	) {
		fpsCounter.increment()
		if (System.nanoTime() > printFpsAt) {
			println("FPS is ${fpsCounter.value}")
			printFpsAt += 500_000_000L
		}
		totalFrames += 1
		if (totalFrames == 10L) launchState()

		renderResources.textBuffer.startFrame(recorder)
		synchronized(gameState.lock()) {
			if (gameState.currentState is ExitState) requestQuit(recorder.stack) else {
				// TODO Propagate content
				val context = RawRenderContext(frame, renderResources, null, gameState, resources)
				renderGame(context)
			}
		}
		// TODO Improve this?
		renderResources.textBuffer.transfer(recorder, renderResources.sharedText)
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		synchronized(gameState.lock()) {
			gameState.currentState = ExitState()
		}
		this.renderResources.destroy()
	}

	private fun launchState() {
		println("Start loading content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
		val content = CompletableFuture<Content>()

		Thread {
			try {
				content.complete(Content.load("mardek/game/content.bits", Bitser(false)))
				println("Loaded content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
			} catch (failed: Throwable) {
				content.completeExceptionally(failed)
				synchronized(gameState.lock()) {
					gameState.currentState = ExitState()
				}
				throw failed
			}
		}//.start()

		val updateLoop = UpdateLoop({
			synchronized(gameState.lock()) {
				gameState.update(content, 10.milliseconds)
			}
		}, 10_000_000L)
		val updateThread = Thread(updateLoop)
		updateThread.isDaemon = true
		updateThread.start()

		val mainThread = Thread.currentThread()

		Thread {
			val audioUpdater = AudioUpdater(gameState)
			val audioLoop = UpdateLoop({ loop ->
				if (mainThread.isAlive) audioUpdater.update()
				else loop.stop()
			}, 10_000_000L)
			audioLoop.run()
			audioUpdater.destroy()
		}.start()
	}

	private fun requestQuit(stack: MemoryStack) {
		val quitEvent = SDL_Event.calloc(stack)
		quitEvent.type(SDL_EVENT_WINDOW_CLOSE_REQUESTED)
		SDL_WindowEvent.nwindowID(quitEvent.address(), SDL_GetWindowID(window.handle))
		assertSdlSuccess(SDL_PushEvent(quitEvent), "PushEvent")
	}
}
