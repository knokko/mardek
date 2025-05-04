package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.SharedDeviceBufferBuilder
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.descriptors.SharedDescriptorPool
import com.github.knokko.boiler.descriptors.SharedDescriptorPoolBuilder
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

const val KIM1_VERTEX_SIZE = 7 * 4

private fun createGraphicsDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val bindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
	boiler.descriptors.binding(bindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)
	boiler.descriptors.createLayout(stack, bindings, "KimComputeDescriptorLayout")
}

private fun createComputeDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val bindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
	for (index in 0 until 3)
		boiler.descriptors.binding(bindings, index, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
	boiler.descriptors.createLayout(stack, bindings, "KimComputeDescriptorLayout")
}

class Kim1Resources(
	private val boiler: BoilerInstance,
	private val framesInFlight: Int,
	renderPass: Long,

	sharedDescriptorPoolBuilder: SharedDescriptorPoolBuilder,
	sharedStorageBuilder: SharedDeviceBufferBuilder,
	storageIntAlignment: Long,
) {

	private val graphicsDescriptorLayout = createGraphicsDescriptorSetLayout(boiler)
	var graphicsDescriptorSet = 0L
		private set

	val graphicsLayout: Long
	val graphicsPipeline: Long

	private val computeDescriptorLayout = createComputeDescriptorSetLayout(boiler)
	lateinit var computeDescriptorSets: LongArray
		private set

	val computeLayout: Long
	val computePipeline: Long

	private val getMiddleBuffer = sharedStorageBuilder.add(1_000_000L, storageIntAlignment)
	lateinit var middleBuffer: VkbBufferRange
		private set

	init {
		sharedDescriptorPoolBuilder.request(graphicsDescriptorLayout, 1)
		sharedDescriptorPoolBuilder.request(computeDescriptorLayout, framesInFlight)
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
				"Kim1", "mardek/renderer/fake-image.vert.spv",
				"mardek/renderer/fake-image.frag.spv"
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

	fun initBuffers() {
		middleBuffer = getMiddleBuffer.get()
	}

	fun initDescriptors(pool: SharedDescriptorPool, spriteBuffer: VkbBufferRange, perFrameBuffer: PerFrameBuffer) {
		this.graphicsDescriptorSet = pool.allocate(graphicsDescriptorLayout, 1)[0]
		this.computeDescriptorSets = pool.allocate(computeDescriptorLayout, framesInFlight)

		stackPush().use { stack ->
			val descriptorWrites = VkWriteDescriptorSet.calloc(3 * framesInFlight + 1, stack)
			for (frame in 0 until framesInFlight) {
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame],
					3 * frame, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, spriteBuffer
				)
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame],
					3 * frame + 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, perFrameBuffer.range.range()
				)
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame],
					3 * frame + 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, middleBuffer
				)
				for (index in 0 until 3) descriptorWrites[3 * frame + index].dstBinding(index)
			}
			boiler.descriptors.writeBuffer(
				stack, descriptorWrites, graphicsDescriptorSet, 3 * framesInFlight, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, middleBuffer
			)
			descriptorWrites.get(3 * framesInFlight).dstBinding(0)

			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), computePipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), graphicsLayout, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), computeLayout, null)
		computeDescriptorLayout.destroy()
		graphicsDescriptorLayout.destroy()
	}
}
