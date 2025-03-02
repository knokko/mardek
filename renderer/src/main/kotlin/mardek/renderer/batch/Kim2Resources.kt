package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

const val KIM2_VERTEX_SIZE = 5 * 4

private fun createDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val bindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
	boiler.descriptors.binding(
		bindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
		VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT
	)
	boiler.descriptors.createLayout(stack, bindings, "Kim2DescriptorLayout")
}

class Kim2Resources(boiler: BoilerInstance, renderPass: Long, spriteBuffer: VkbBufferRange) {

	private val descriptorLayout = createDescriptorSetLayout(boiler)
	private val descriptorPool = descriptorLayout.createPool(1, 0, "Kim2DescriptorPool")
	val descriptorSet = descriptorPool.allocate(1)[0]

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8)

			this.pipelineLayout = boiler.pipelines.createLayout(
				pushConstants, "Kim2PipelineLayout", descriptorLayout.vkDescriptorSetLayout
			)

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, KIM2_VERTEX_SIZE, VK_VERTEX_INPUT_RATE_INSTANCE)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(4, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_SFLOAT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_SINT, 12)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_SFLOAT, 16)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Kim2", "mardek/renderer/kim2.vert.spv",
				"mardek/renderer/kim2.frag.spv"
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
			builder.ciPipeline.renderPass(renderPass)
			builder.ciPipeline.subpass(0)
			this.graphicsPipeline = builder.build("Kim2GraphicsPipeline")

			val writes = VkWriteDescriptorSet.calloc(1, stack)
			boiler.descriptors.writeBuffer(stack, writes, descriptorSet, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, spriteBuffer)

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
