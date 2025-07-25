package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline
import com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline
import com.github.knokko.vk2d.pipeline.Vk2dKimPipeline
import com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.resource.Vk2dTextBuffer
import mardek.content.ui.TitleScreenContent
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass

class RenderResources(
	val boiler: BoilerInstance,
	private val pipelineContext: Vk2dPipelineContext,
	combiner: MemoryCombiner,
	descriptors: DescriptorCombiner,
	numFramesInFlight: Int,
) {
	val shared = Vk2dShared(boiler)
	val sharedText = Vk2dSharedText(boiler)
	val colorPipeline = Vk2dColorPipeline(pipelineContext)
	val ovalPipeline = Vk2dOvalPipeline(pipelineContext)
	val imagePipeline = Vk2dImagePipeline(pipelineContext, shared)
	val kim3Pipeline = Vk2dKimPipeline(pipelineContext, shared, 3)
	val glyphPipeline = Vk2dGlyphPipeline(pipelineContext, sharedText)
	val textBuffer = Vk2dTextBuffer(boiler, combiner, numFramesInFlight)

	init {
		textBuffer.requestDescriptorSets(sharedText, descriptors)
	}

	fun postInit(bundle: Vk2dResourceBundle, titleScreen: TitleScreenContent) {
		// TODO Support multiple fonts
		textBuffer.initializeDescriptorSets(boiler, bundle.getFont(titleScreen.smallFont.index))
	}

	fun destroy() {
		glyphPipeline.destroy(boiler)
		colorPipeline.destroy(boiler)
		ovalPipeline.destroy(boiler)
		imagePipeline.destroy(boiler)
		kim3Pipeline.destroy(boiler)
		sharedText.destroy(boiler)
		shared.destroy(boiler)
		stackPush().use { stack ->
			vkDestroyRenderPass(
				boiler.vkDevice(), pipelineContext.vkRenderPass,
				CallbackUserData.RENDER_PASS.put(stack, boiler)
			)
		}
	}
}
