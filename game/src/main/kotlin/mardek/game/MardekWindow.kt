package mardek.game

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.update.UpdateCounter
import com.github.knokko.update.UpdateLoop
import com.github.knokko.vk2d.Vk2dConfig
import com.github.knokko.vk2d.Vk2dWindow
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import mardek.audio.AudioUpdater
import mardek.content.Content
import mardek.renderer.PerFrameResources
import mardek.renderer.RenderManager
import mardek.state.VideoSettings
import mardek.state.ExitState
import mardek.state.GameStateManager
import org.lwjgl.system.MemoryStack
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

class MardekWindow(
	private val gameState: GameStateManager,
	private val videoSettings: VideoSettings,
	window: VkbWindow,
) : Vk2dWindow(window, videoSettings.capFps, videoSettings.delayRendering) {

	private var totalFrames = 0L
	private lateinit var renderManager: RenderManager
	private lateinit var swapchainResources: MardekSwapchainResources
	lateinit var perFrame: Array<PerFrameResources>

	private val fpsCounter = UpdateCounter()

	override fun initialResourceBundle(): InputStream {
		return MardekWindow::class.java.getResourceAsStream("title-screen.vk2d")!!
	}

	override fun setupConfig(config: Vk2dConfig) = RenderManager.initPipelinesConfig(config)

	override fun createResources(boiler: BoilerInstance, combiner: MemoryCombiner, descriptors: DescriptorCombiner) {
		super.createResources(boiler, combiner, descriptors)
		this.perFrame = (0 until numFramesInFlight).map {
			PerFrameResources(
				areaBlurDescriptors = pipelines.blur.claimResources(1, instance, descriptors)[0],
				sectionsBlurDescriptors = pipelines.blur.claimResources(1, instance, descriptors)[0],
				actionBarBlurDescriptors = pipelines.blur.claimResources(1, instance, descriptors)[0]
			)
		}.toTypedArray()
	}

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)
		val pipelineContext = Vk2dPipelineContext.renderPass(boiler, vkRenderPass)
		this.renderManager = RenderManager(
			boiler, videoSettings, resources, pipelineContext, pipelines,
		)
		this.swapchainResources = MardekSwapchainResources(
			boiler, pipelines.blur, window.properties.surfaceFormat, vkRenderPass
		)
	}

	override fun renderFrame(
		frame: Vk2dSwapchainFrame,
		frameIndex: Int,
		recorder: CommandRecorder,
		swapchainImage: AcquiredImage,
		boiler: BoilerInstance
	) {
		fpsCounter.increment()
		totalFrames += 1
		if (totalFrames == 10L) launchState()

		synchronized(gameState.lock()) {
			val currentState = gameState.currentState
			if (currentState is ExitState) window.requestClose()
			renderManager.renderFrame(
				gameState, frame, recorder,
				textBuffer, perFrameDescriptorSet,
				swapchainResources.getSwapchainAssociation(swapchainImage),
				perFrame[frameIndex], fpsCounter.value,
			)

			presentMode = choosePresentMode(window, videoSettings.capFps)
		}
	}

	override fun cleanUp(boiler: BoilerInstance) {
		renderManager.waitUntilStable()
		super.cleanUp(boiler)
		synchronized(gameState.lock()) {
			gameState.currentState = ExitState()
		}
		renderManager.cleanUp()
	}

	private fun launchState() {
		println("Start loading content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
		val content = CompletableFuture<Content>()

		Thread {
			try {
				content.complete(Content.load("mardek/game/content.bits", Bitser(false)))
				println("Loaded content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
				renderManager.content = content.get()
			} catch (failed: Throwable) {
				content.completeExceptionally(failed)
				synchronized(gameState.lock()) {
					gameState.currentState = ExitState()
				}
				throw failed
			}
		}.start()

		Thread {
			try {
				renderManager.loadMainResources(
					MardekWindow::class.java.getResourceAsStream("content.vk2d")!!
				)
				println("Loaded main resources after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
			} catch (failed: Throwable) {
				synchronized(gameState.lock()) {
					gameState.currentState = ExitState()
				}
				throw failed
			}
		}.start()

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
			UpdateLoop({ loop ->
				if (mainThread.isAlive) audioUpdater.update()
				else loop.stop()
			}, 10_000_000L).run()
			audioUpdater.destroy()
		}.start()
	}
}
