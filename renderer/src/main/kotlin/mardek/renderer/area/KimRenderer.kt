package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.assets.sprite.KimSprite
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import org.lwjgl.vulkan.VkWriteDescriptorSet

private const val VERTEX_SIZE = 7 * 4

private fun createComputeDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val bindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
	for (index in 0 until 3)
		boiler.descriptors.binding(bindings, index, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
	boiler.descriptors.createLayout(stack, bindings, "KimComputeDescriptorLayout")
}

class KimRenderer(
	private val boiler: BoilerInstance,
	private val descriptorSetLayout: Long,
	private val descriptorSet: Long,
	private val kimSpritesBuffer: VkbBufferRange,
	private val framesInFlight: Int,
	private val maxRequestCount: Int,
	private val targetImageFormat: Int,
) {

	private val vertexBuffers = (0 until framesInFlight).map {
		boiler.buffers.createMapped(VERTEX_SIZE.toLong() * maxRequestCount, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "KimVertices$it")
	}

	private val requests = ArrayList<KimRequest>()

	private val graphicsLayout: Long
	private val graphicsPipeline: Long

	private val computeDescriptorLayout = createComputeDescriptorSetLayout(boiler)
	private val computeDescriptorPool = computeDescriptorLayout.createPool(framesInFlight, 0, "Kim1SamplePool")
	private val computeDescriptorSets = computeDescriptorPool.allocate(framesInFlight)

	private val computeLayout: Long
	private val computePipeline: Long

	private val offsetBuffers = (0 until framesInFlight).map { boiler.buffers.createMapped(
		4L * maxRequestCount, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "KimOffsetBuffer"
	) }
	val middleBuffer = boiler.buffers.create(
		4L * 16 * 16 * maxRequestCount, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "KimMiddleBuffer"
	)!!

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8)
			this.graphicsLayout = boiler.pipelines.createLayout(pushConstants, "Kim1PipelineLayout", descriptorSetLayout)

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, VERTEX_SIZE, VK_VERTEX_INPUT_RATE_INSTANCE)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(5, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_UINT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_SINT, 16)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_SINT, 20)
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_SFLOAT, 24)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Kim1", "mardek/renderer/area/basic.vert.spv",
				"mardek/renderer/area/basic.frag.spv"
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
			builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
			this.graphicsPipeline = builder.build("Kim1GraphicsPipeline")

			val computeConstants = VkPushConstantRange.calloc(1, stack)
			computeConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 16)
			this.computeLayout = boiler.pipelines.createLayout(
				computeConstants, "Kim1ComputeLayout", computeDescriptorLayout.vkDescriptorSetLayout
			)
			this.computePipeline = boiler.pipelines.createComputePipeline(
				computeLayout, "mardek/renderer/area/kim1-decompressor.comp.spv", "Kim1ComputePipeline"
			)

			val descriptorWrites = VkWriteDescriptorSet.calloc(3, stack)
			for (frame in 0 until framesInFlight) {
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame], 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, kimSpritesBuffer
				)
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame], 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, offsetBuffers[frame].fullRange()
				)
				boiler.descriptors.writeBuffer(
					stack, descriptorWrites, computeDescriptorSets[frame], 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, middleBuffer.fullRange()
				)
				vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
			}
		}
	}

	fun begin() {
		if (requests.isNotEmpty()) throw IllegalStateException("Did you forget to call end() ?")
	}

	fun render(request: KimRequest) {
		requests.add(request)
	}

	private val offsetMap = mutableMapOf<Int, Int>()
	private val sizeMap = mutableMapOf<Pair<Int, Int>, MutableList<KimRequest>>()

	fun recordBeforeRenderpass(recorder: CommandRecorder, frameIndex: Int) {
		if (requests.isEmpty()) return
		if (offsetMap.isNotEmpty() || sizeMap.isNotEmpty()) throw IllegalStateException("Bad call order")

		val readUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
		val writeUsage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT)
		recorder.bufferBarrier(middleBuffer.fullRange(), readUsage, writeUsage)

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline)
		recorder.bindComputeDescriptors(computeLayout, computeDescriptorSets[frameIndex])

		for (request in requests) {
			val size = Pair(request.sprite.width, request.sprite.height)
			sizeMap.computeIfAbsent(size) { ArrayList() }.add(request)
		}

		val offsetBuffer = offsetBuffers[frameIndex].fullMappedRange().byteBuffer()
		var nextResultOffset = 0
		var nextOffsetOffset = 0
		for ((size, requests) in sizeMap) {
			vkCmdPushConstants(
				recorder.commandBuffer, computeLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0,
				recorder.stack.ints(size.first, size.second, nextResultOffset, nextOffsetOffset)
			)

			var counter = 0
			for (request in requests) {
				if (offsetMap.containsKey(request.sprite.offset)) continue
				offsetMap[request.sprite.offset] = nextResultOffset
				nextResultOffset += size.first * size.second
				offsetBuffer.putInt(request.sprite.offset)
				counter += 1
			}
			nextOffsetOffset += counter
			vkCmdDispatch(recorder.commandBuffer, size.first, size.second, counter)
		}

		recorder.bufferBarrier(middleBuffer.fullRange(), writeUsage, readUsage)
	}

	fun recordDuringRenderpass(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		if (requests.isEmpty()) return
		if (offsetMap.isEmpty()) throw IllegalStateException("Bad call order")

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)
		recorder.bindGraphicsDescriptors(graphicsLayout, descriptorSet)
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(vertexBuffers[frameIndex].vkBuffer),
			recorder.stack.longs(0L)
		)
		vkCmdPushConstants(
			recorder.commandBuffer, graphicsLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, recorder.stack.ints(targetImage.width, targetImage.height)
		)

		val vertexBuffer = vertexBuffers[frameIndex].mappedRange(0L, VERTEX_SIZE.toLong() * requests.size).byteBuffer()
		for (request in requests) {
			vertexBuffer.putInt(request.x).putInt(request.y)
			vertexBuffer.putInt(request.sprite.width).putInt(request.sprite.height).putInt(request.scale)
			vertexBuffer.putInt(offsetMap[request.sprite.offset]!!).putFloat(request.opacity)
		}
		vkCmdDraw(recorder.commandBuffer, 6, requests.size, 0, 0)
		requests.clear()
		offsetMap.clear()
		sizeMap.clear()
	}

	fun destroy() {
		for (buffer in vertexBuffers) buffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), computePipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), graphicsLayout, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), computeLayout, null)
		computeDescriptorPool.destroy()
		computeDescriptorLayout.destroy()
		for (buffer in offsetBuffers) buffer.destroy(boiler)
		middleBuffer.destroy(boiler)

		if (requests.isNotEmpty() || offsetMap.isNotEmpty()) throw IllegalStateException("Did you forget to call end() ?")
	}
}

class KimRequest(val x: Int, val y: Int, val scale: Int, val sprite: KimSprite, val opacity: Float)
