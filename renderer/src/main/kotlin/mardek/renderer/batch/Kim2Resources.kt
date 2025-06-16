package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.VkbBuffer
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

const val KIM2_VERTEX_SIZE = 5 * 4

private fun createDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val builder = DescriptorSetLayoutBuilder(stack, 1)
	builder.set(
		0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
		VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT
	)
	builder.build(boiler, "Kim2DescriptorLayout")
}

class Kim2Resources(
	private val boiler: BoilerInstance,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
) {

	private val descriptorLayout = createDescriptorSetLayout(boiler)
	var descriptorSet = 0L
		private set

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		descriptorCombiner.addSingle(descriptorLayout) { descriptorSet = it }
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
				"Kim2", "mardek/renderer/",
				"kim2.vert.spv", "kim2.frag.spv"
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
		}
	}

	fun prepare(spriteBuffer: VkbBuffer) {
		stackPush().use { stack ->
			val updater = DescriptorUpdater(stack, 1)
			updater.writeStorageBuffer(0, descriptorSet, 0, spriteBuffer)
			updater.update(boiler)
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), descriptorLayout.vkDescriptorSetLayout, null)
	}
}
