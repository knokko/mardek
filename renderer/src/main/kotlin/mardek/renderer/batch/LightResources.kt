package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

const val LIGHT_VERTEX_SIZE = 4 * 4

class LightResources(
	boiler: BoilerInstance,
	targetImageFormat: Int,
) {

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8)

			this.pipelineLayout = boiler.pipelines.createLayout(pushConstants, "LightPipelineLayout")

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, LIGHT_VERTEX_SIZE, VK_VERTEX_INPUT_RATE_INSTANCE)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_UINT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 12)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Light", "mardek/renderer/light.vert.spv",
				"mardek/renderer/light.frag.spv"
			)
			builder.ciPipeline.pVertexInputState(ciVertex)
			builder.simpleInputAssembly()
			builder.dynamicViewports(1)
			builder.simpleRasterization(VK_CULL_MODE_NONE)
			builder.noMultisampling()
			builder.noDepthStencil()
			builder.simpleColorBlending(1)
			builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
			builder.ciPipeline.layout(pipelineLayout)
			builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
			this.graphicsPipeline = builder.build("LightGraphicsPipeline")
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
	}
}
