package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

private fun createDescriptorSetLayout(boiler: BoilerInstance) = MemoryStack.stackPush().use { stack ->
	val bindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
	boiler.descriptors.binding(bindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)
	boiler.descriptors.createLayout(stack, bindings, "ColorGridDescriptorLayout")
}

// TODO Add CombinedDescriptorPool to vk-boiler
class ColorGridResources(
	boiler: BoilerInstance,
	targetImageFormat: Int,
	perFrame: PerFrameBuffer,
) {

	private val descriptorLayout = createDescriptorSetLayout(boiler)
	private val descriptorPool = descriptorLayout.createPool(1, 0, "Kim2DescriptorPool")
	val descriptorSet = descriptorPool.allocate(1)[0]

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, 0, 36)

			this.pipelineLayout = boiler.pipelines.createLayout(
				pushConstants, "ColorGridPipelineLayout", descriptorLayout.vkDescriptorSetLayout
			)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"ColorGrid", "mardek/renderer/color-grid.vert.spv",
				"mardek/renderer/color-grid.frag.spv"
			)
			builder.noVertexInput()
			builder.simpleInputAssembly()
			builder.dynamicViewports(1)
			builder.simpleRasterization(VK_CULL_MODE_NONE)
			builder.noMultisampling()
			builder.noDepthStencil()
			builder.noColorBlending(1)
			builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
			builder.ciPipeline.layout(pipelineLayout)
			builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
			this.graphicsPipeline = builder.build("ColorGridPipeline")

			val writes = VkWriteDescriptorSet.calloc(1, stack)
			boiler.descriptors.writeBuffer(
				stack, writes, descriptorSet, 0,
				VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, perFrame.range.range()
			)

			vkUpdateDescriptorSets(boiler.vkDevice(), writes, null)
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		descriptorPool.destroy()
		descriptorLayout.destroy()
	}
}