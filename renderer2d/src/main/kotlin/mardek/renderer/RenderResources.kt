package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.Vk2dShared
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline
import com.github.knokko.vk2d.pipeline.Vk2dKimPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass

class RenderResources(val boiler: BoilerInstance, private val pipelineContext: Vk2dPipelineContext) {
	val shared = Vk2dShared(boiler)
	val colorPipeline = Vk2dColorPipeline(pipelineContext)
	val imagePipeline = Vk2dImagePipeline(pipelineContext, shared)
	val kim3Pipeline = Vk2dKimPipeline(pipelineContext, shared, 3)

	fun destroy() {
		colorPipeline.destroy(boiler)
		imagePipeline.destroy(boiler)
		kim3Pipeline.destroy(boiler)
		shared.destroy(boiler)
		stackPush().use { stack ->
			vkDestroyRenderPass(
				boiler.vkDevice(), pipelineContext.vkRenderPass,
				CallbackUserData.RENDER_PASS.put(stack, boiler)
			)
		}
	}
}
