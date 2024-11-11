package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.DeviceVkbBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.pipelines.ShaderInfo
import com.github.knokko.boiler.synchronization.ResourceUsage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkSpecializationInfo
import org.lwjgl.vulkan.VkSpecializationMapEntry
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.io.BufferedInputStream
import java.io.DataInputStream

private fun simplePipelineBuilder(boiler: BoilerInstance, stack: MemoryStack, targetImageFormat: Int): GraphicsPipelineBuilder {
	val builder = GraphicsPipelineBuilder(boiler, stack)
	// Shader stages
	// Vertex input
	builder.simpleInputAssembly()
	builder.dynamicViewports(1)
	builder.simpleRasterization(VK_CULL_MODE_NONE)
	builder.noMultisampling()
	builder.noDepthStencil()
	builder.simpleColorBlending(1)
	builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
	// Pipeline layout
	builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
	return builder
}

class AreaResources(boiler: BoilerInstance, resourcePath: String, targetImageFormat: Int) {

	private val deviceBuffer: DeviceVkbBuffer
	private val descriptorSetLayout: VkbDescriptorSetLayout
	private val descriptorPool: HomogeneousDescriptorPool
	val descriptorSet: Long

	val pipelineLayout: Long
	val lowTilesPipeline: Long
	val highTilesPipeline: Long

	init {
		val startTime = System.nanoTime()
		val input = DataInputStream(BufferedInputStream(AreaResources::class.java.classLoader.getResourceAsStream(resourcePath)))

		val generalSpritesSize = input.readInt()
		val highTileSpritesSize = input.readInt()
		val tileGridsSize = input.readInt()

		deviceBuffer = boiler.buffers.create(
			4L * (generalSpritesSize + highTileSpritesSize + tileGridsSize),
			VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Area Buffer"
		)
		val stagingBuffer = boiler.buffers.createMapped(
			deviceBuffer.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "Area Staging Buffer"
		)

		val stagingInts = stagingBuffer.fullMappedRange().intBuffer()
		for (counter in 0 until generalSpritesSize) stagingInts.put(input.readInt())
		for (counter in 0 until highTileSpritesSize) stagingInts.put(input.readInt())
		for (counter in 0 until tileGridsSize) stagingInts.put(input.readInt())

		input.close()

		val commands = SingleTimeCommands(boiler)
		commands.submit("Area Staging Transfer") { recorder ->
			recorder.copyBufferRanges(stagingBuffer.fullRange(), deviceBuffer.fullRange())
			recorder.bufferBarrier(deviceBuffer.fullRange(), ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_VERTEX_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			))
		}.awaitCompletion()
		commands.destroy()
		stagingBuffer.destroy(boiler)

		stackPush().use { stack ->
			val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
			boiler.descriptors.binding(descriptorBindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)

			this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "AreaDescriptorLayout")
			this.descriptorPool = descriptorSetLayout.createPool(1, 0, "AreaDescriptorPool")
			this.descriptorSet = descriptorPool.allocate(1)[0]

			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 52)

			this.pipelineLayout = boiler.pipelines.createLayout(
				pushConstants, "AreaPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
			)
			val specializationEntries = VkSpecializationMapEntry.calloc(3, stack)
			specializationEntries.get(0).set(0, 0, 4)
			specializationEntries.get(1).set(1, 4, 4)
			specializationEntries.get(2).set(2, 8, 4)

			val specialization = VkSpecializationInfo.calloc(stack)
			specialization.pMapEntries(specializationEntries)
			specialization.pData(
				stack.calloc(12).putInt(generalSpritesSize).putInt(highTileSpritesSize).putInt(tileGridsSize).flip()
			)

			val tilesVertexModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles.vert.spv", "TilesVertexShader"
			)
			val tilesVertexShader = ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, tilesVertexModule, null)

			val lowTilesFragmentModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles-low.frag.spv", "LowTilesFragmentShader"
			)
			val lowTilesFragmentShader = ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, lowTilesFragmentModule, specialization)

			val lowTiles = simplePipelineBuilder(boiler, stack, targetImageFormat)
			lowTiles.shaderStages(tilesVertexShader, lowTilesFragmentShader)
			lowTiles.noVertexInput()
			lowTiles.ciPipeline.layout(pipelineLayout)
			this.lowTilesPipeline = lowTiles.build("LowTilesPipeline")

			val highTilesFragmentModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles-high.frag.spv", "HighTilesFragmentShader"
			)
			val highTilesFragmentShader = ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, highTilesFragmentModule, specialization)

			val highTiles = simplePipelineBuilder(boiler, stack, targetImageFormat)
			highTiles.shaderStages(tilesVertexShader, highTilesFragmentShader)
			highTiles.noVertexInput()
			highTiles.ciPipeline.layout(pipelineLayout)
			this.highTilesPipeline = highTiles.build("HighTilesPipeline")

			vkDestroyShaderModule(boiler.vkDevice(), tilesVertexModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), lowTilesFragmentModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), highTilesFragmentModule, null)

			val descriptorWrites = VkWriteDescriptorSet.calloc(1, stack)
			boiler.descriptors.writeBuffer(
				stack, descriptorWrites, descriptorSet, 0,
				VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, deviceBuffer.fullRange()
			)
			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
		}
		println("Took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy(boiler: BoilerInstance) {
		deviceBuffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), lowTilesPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), highTilesPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
	}
}
