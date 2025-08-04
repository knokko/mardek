package mardek.game

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.update.UpdateCounter
import com.github.knokko.update.UpdateLoop
import com.github.knokko.vk2d.Vk2dConfig
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.Vk2dWindow
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.resource.Vk2dResourceLoader
import mardek.audio.AudioUpdater
import mardek.content.Content
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphPipeline
import mardek.renderer.renderGame
import mardek.state.ExitState
import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.title.TitleScreenState
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

class MardekWindow(
	private val gameState: GameStateManager, window: VkbWindow, capFps: Boolean
) : Vk2dWindow(window, capFps) {

	private var totalFrames = 0L
	private lateinit var textPipeline: MardekGlyphPipeline

	private lateinit var content: Content
	private lateinit var mainResources: Vk2dResourceBundle
	private lateinit var mainResourceMemory: MemoryBlock
	private var mainDescriptorPool = VK_NULL_HANDLE

	private val fpsCounter = UpdateCounter()
	private var printFpsAt = System.nanoTime() + 1500_000_000L

	override fun initialResourceBundle(): InputStream {
		return MardekWindow::class.java.getResourceAsStream("title-screen.vk2d")!!
	}

	override fun setupConfig(config: Vk2dConfig) {
		config.color = true
		config.oval = true
		config.image = true
		config.kim3 = true
		config.text = true
	}

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)
		val pipelineContext = Vk2dPipelineContext.renderPass(boiler, stack, vkRenderPass)
		this.textPipeline = MardekGlyphPipeline(pipelineContext, instance)
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
		if (totalFrames == 10L) launchState(boiler)

		synchronized(gameState.lock()) {
			val currentState = gameState.currentState
			if (currentState is ExitState) window.requestClose()
			if (
				this::content.isInitialized &&
				this::mainResources.isInitialized &&
				currentState is InGameState
			) {
				val context = RenderContext(
					frame, pipelines, content, gameState,
					currentState.campaign, mainResources
				)
				renderGame(context)
			} else {
				val context = RawRenderContext(
					frame, pipelines, textPipeline, textBuffer, perFrameDescriptorSet, recorder,
					null, gameState, resources
				)
				renderGame(context)
			}
		}
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		synchronized(gameState.lock()) {
			gameState.currentState = ExitState()
		}
		textPipeline.destroy(boiler)
		if (this::mainResourceMemory.isInitialized) mainResourceMemory.destroy(boiler)
		if (mainDescriptorPool != VK_NULL_HANDLE) {
			stackPush().use { stack ->
				vkDestroyDescriptorPool(
					boiler.vkDevice(), mainDescriptorPool,
					CallbackUserData.DESCRIPTOR_POOL.put(stack, boiler)
				)
			}
		}
	}

	private fun launchState(boiler: BoilerInstance) {
		println("Start loading content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
		val content = CompletableFuture<Content>()

		Thread {
			try {
				content.complete(Content.load("mardek/game/content.bits", Bitser(false)))
				println("Loaded content after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
				this.content = content.get()
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
				val loader = Vk2dResourceLoader(instance, MardekWindow::class.java.getResourceAsStream("content.vk2d"))
				val combiner = MemoryCombiner(boiler, "MainContent")
				loader.claimMemory(combiner)
				this.mainResourceMemory = combiner.build(false)

				loader.prepareStaging()

				val descriptors = DescriptorCombiner(boiler)
				SingleTimeCommands.submit(boiler, "Load MainContent") { recorder ->
					loader.performStaging(recorder, descriptors)
				}.destroy()

				this.mainDescriptorPool = descriptors.build("MainDescriptors")
				this.mainResources = loader.finish()
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
			val audioLoop = UpdateLoop({ loop ->
				if (mainThread.isAlive) audioUpdater.update()
				else loop.stop()
			}, 10_000_000L)
			audioLoop.run()
			audioUpdater.destroy()
		}.start()
	}
}
