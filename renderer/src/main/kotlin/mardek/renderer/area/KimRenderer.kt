package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

private const val VERTEX_SIZE = 5 * 4

class KimRenderer(
	private val boiler: BoilerInstance,
	private val descriptorSetLayout: Long,
	private val framesInFlight: Int,
	private val maxRequestCount: Int,
	private val targetImageFormat: Int,
) {

	private val vertexBuffers = (0 until framesInFlight).map {
		boiler.buffers.createMapped(VERTEX_SIZE.toLong() * maxRequestCount, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "KimVertices$it")
	}

	private val requests = ArrayList<KimRequest>()

	private val pipelineLayout: Long
	private val graphicsPipeline: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8)
			this.pipelineLayout = boiler.pipelines.createLayout(pushConstants, "Kim1PipelineLayout", descriptorSetLayout)

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, VERTEX_SIZE, VK_VERTEX_INPUT_RATE_INSTANCE)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(4, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_SINT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_SINT, 12)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_SFLOAT, 16)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Kim1", "mardek/renderer/area/kim1.vert.spv",
				"mardek/renderer/area/kim1.frag.spv"
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
			this.graphicsPipeline = builder.build("Kim1Pipeline")
		}
	}

	fun begin() {
		if (requests.isNotEmpty()) throw IllegalStateException("Did you forget to call end() ?")
	}

	fun render(request: KimRequest) {
		requests.add(request)
	}

	fun end(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int, descriptorSet: Long) {
		if (requests.isEmpty()) return

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSet)
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(vertexBuffers[frameIndex].vkBuffer),
			recorder.stack.longs(0L)
		)
		vkCmdPushConstants(
			recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, recorder.stack.ints(targetImage.width, targetImage.height)
		)

		val vertexBuffer = vertexBuffers[frameIndex].mappedRange(0L, VERTEX_SIZE.toLong() * requests.size).byteBuffer()
		for (request in requests) {
			vertexBuffer.putInt(request.x).putInt(request.y).putInt(request.scale)
			vertexBuffer.putInt(request.spriteOffset).putFloat(request.opacity)
		}
		vkCmdDraw(recorder.commandBuffer, 6, requests.size, 0, 0)
		requests.clear()
	}

	fun destroy() {
		for (buffer in vertexBuffers) buffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		if (requests.isNotEmpty()) throw IllegalStateException("Did you forget to call end() ?")
	}
}

class KimRequest(val x: Int, val y: Int, val scale: Int, val spriteOffset: Int, val opacity: Float)
