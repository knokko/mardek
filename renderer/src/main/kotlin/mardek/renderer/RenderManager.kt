package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.Vk2dConfig
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.resource.Vk2dResourceLoader
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.content.Content
import mardek.state.GameStateManager
import mardek.state.VideoSettings
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import java.io.InputStream
import java.util.concurrent.CompletableFuture

class RenderManager(
	private val boiler: BoilerInstance,
	private val videoSettings: VideoSettings,
	private val titleScreenResources: Vk2dResourceBundle,
	pipelineContext: Vk2dPipelineContext,
	basePipelines: Vk2dPipelines,
) {

	val pipelines = MardekPipelines(basePipelines, pipelineContext)
	private val vk2d = basePipelines.instance

	lateinit var content: Content
	private lateinit var mainResourceMemory: MemoryBlock
	private lateinit var mainResources: Vk2dResourceBundle
	private var mainDescriptorPool = VK_NULL_HANDLE

	private var shouldCancelLoading = false
	private var isLoading: CompletableFuture<Unit>? = null

	fun loadMainResources(contentInput: InputStream) {
		synchronized(this) {
			if (shouldCancelLoading) return
			isLoading = CompletableFuture()
		}

		try {
			val loader = Vk2dResourceLoader(vk2d, contentInput)
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
		} finally {
			isLoading!!.complete(Unit)
		}
	}

	fun renderFrame(
		gameState: GameStateManager, frame: Vk2dSwapchainFrame, recorder: CommandRecorder,
		textBuffer: Vk2dTextBuffer, perFrameDescriptorSet: Long,
		framebuffers: MardekFramebuffers, perFrame: PerFrameResources, currentFps: Long,
	) {
		val currentState = gameState.currentState
		val fullRenderContext = if (this::content.isInitialized && this::mainResources.isInitialized) {
			val campaign = if (currentState is InGameState) currentState.campaign else CampaignState()
			RenderContext(
				frame, frame.swapchainStage, framebuffers, perFrame,
				pipelines, textBuffer, perFrameDescriptorSet, recorder, content, gameState,
				campaign, mainResources, videoSettings, currentFps,
			)
		} else null

		if (fullRenderContext != null && currentState is InGameState) {
			renderGame(fullRenderContext)
		} else {
			val partialRenderContext = RawRenderContext(
				frame.swapchainStage, pipelines, textBuffer, perFrameDescriptorSet, recorder,
				null, gameState, titleScreenResources, videoSettings, currentFps,
			)
			renderGame(partialRenderContext, fullRenderContext)
		}
	}

	fun waitUntilStable() {
		synchronized(this) {
			if (isLoading != null) {
				try {
					isLoading!!.get()
				} catch (_: Throwable) {}
			} else shouldCancelLoading = true
		}
	}

	fun cleanUp() {
		pipelines.destroy(boiler)
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

	companion object {
		fun initPipelinesConfig(config: Vk2dConfig) {
			config.color = true
			config.oval = true
			config.image = true
			config.kim3 = true
			config.text = true
			config.blur = true
		}
	}
}
