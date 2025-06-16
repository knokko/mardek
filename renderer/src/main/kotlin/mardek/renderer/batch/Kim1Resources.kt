package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.VkbBuffer
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

const val KIM1_VERTEX_SIZE = 7 * 4

private fun createGraphicsDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val builder = DescriptorSetLayoutBuilder(stack, 1)
	builder.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)
	builder.build(boiler, "KimGraphicsDescriptorLayout")
}

private fun createComputeDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val builder = DescriptorSetLayoutBuilder(stack, 3)
	for (index in 0 until 3) {
		builder.set(index, index, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
	}
	builder.build(boiler, "KimComputeDescriptorLayout")
}

class Kim1Resources(
	private val boiler: BoilerInstance,
	private val framesInFlight: Int,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
	persistentCombiner: MemoryCombiner,
) {

	private val graphicsDescriptorLayout = createGraphicsDescriptorSetLayout(boiler)
	var graphicsDescriptorSet = 0L
		private set

	val graphicsLayout: Long
	val graphicsPipeline: Long

	private val computeDescriptorLayout = createComputeDescriptorSetLayout(boiler)
	val computeDescriptorSets = descriptorCombiner.addMultiple(computeDescriptorLayout, framesInFlight)!!

	val computeLayout: Long
	val computePipeline: Long

	val middleBuffer = persistentCombiner.addBuffer(
		1_000_000L, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
		VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
	)!!

	init {
		descriptorCombiner.addSingle(graphicsDescriptorLayout) { graphicsDescriptorSet = it }

		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8)
			this.graphicsLayout = boiler.pipelines.createLayout(
				pushConstants, "Kim1GraphicsLayout", graphicsDescriptorLayout.vkDescriptorSetLayout
			)

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, KIM1_VERTEX_SIZE, VK_VERTEX_INPUT_RATE_INSTANCE)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(5, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_UINT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_SFLOAT, 16)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_SINT, 20)
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_SFLOAT, 24)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Kim1", "mardek/renderer/",
				"fake-image.vert.spv", "fake-image.frag.spv"
			)
			builder.ciPipeline.pVertexInputState(ciVertex)
			builder.simpleInputAssembly()
			builder.dynamicViewports(1)
			builder.simpleRasterization(VK_CULL_MODE_NONE)
			builder.noMultisampling()
			builder.noDepthStencil()
			builder.simpleColorBlending(1)
			builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
			builder.ciPipeline.layout(graphicsLayout)
			builder.ciPipeline.renderPass(renderPass)
			builder.ciPipeline.subpass(0)
			this.graphicsPipeline = builder.build("Kim1GraphicsPipeline")

			val computeConstants = VkPushConstantRange.calloc(1, stack)
			computeConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 16)
			this.computeLayout = boiler.pipelines.createLayout(
				computeConstants, "Kim1ComputeLayout", computeDescriptorLayout.vkDescriptorSetLayout
			)
			this.computePipeline = boiler.pipelines.createComputePipeline(
				computeLayout, "mardek/renderer/kim1-decompressor.comp.spv", "Kim1ComputePipeline"
			)
		}
	}

	fun prepare(perFrameBuffer: PerFrameBuffer, spriteBuffer: VkbBuffer) {
		stackPush().use { stack ->
			val updater = DescriptorUpdater(stack, 1 + 3 * framesInFlight)
			for (frame in 0 until framesInFlight) {
				updater.writeStorageBuffer(
					3 * frame, computeDescriptorSets[frame],
					0, spriteBuffer
				)
				updater.writeStorageBuffer(
					3 * frame + 1, computeDescriptorSets[frame],
					1, perFrameBuffer.buffer
				)
				updater.writeStorageBuffer(
					3 * frame + 2, computeDescriptorSets[frame],
					2, middleBuffer
				)
			}
			updater.writeStorageBuffer(3 * framesInFlight, graphicsDescriptorSet, 0, middleBuffer)
			updater.update(boiler)
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), computePipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), graphicsLayout, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), computeLayout, null)
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), computeDescriptorLayout.vkDescriptorSetLayout, null)
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), graphicsDescriptorLayout.vkDescriptorSetLayout, null)
	}
}
