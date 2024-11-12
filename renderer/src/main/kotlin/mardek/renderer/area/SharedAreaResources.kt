package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.DeviceVkbBuffer
import com.github.knokko.boiler.buffers.MappedVkbBufferRange
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.pipelines.ShaderInfo
import com.github.knokko.boiler.synchronization.ResourceUsage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkSpecializationInfo
import org.lwjgl.vulkan.VkSpecializationMapEntry
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
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

private fun loadMapsAndSprites(
	boiler: BoilerInstance, resourcePath: String,
	specialization: VkSpecializationInfo, stack: MemoryStack
): DeviceVkbBuffer {
	val input = DataInputStream(BufferedInputStream(SharedAreaResources::class.java.classLoader.getResourceAsStream(resourcePath)))

	val generalSpritesSize = input.readInt()
	val highTileSpritesSize = input.readInt()
	val tileGridsSize = input.readInt()

	val deviceBuffer = boiler.buffers.create(
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

	specialization.pData(
		stack.calloc(12).putInt(generalSpritesSize).putInt(highTileSpritesSize).putInt(tileGridsSize).flip()
	)

	return deviceBuffer
}

private fun createDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
	boiler.descriptors.binding(
		descriptorBindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
		VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT
	)

	boiler.descriptors.createLayout(stack, descriptorBindings, "AreaDescriptorLayout")
}

private fun createEntityBuffers(boiler: BoilerInstance, framesInFlight: Int): List<MappedVkbBufferRange> {
	val builder = SharedMappedBufferBuilder(boiler)

	val alignment = 4L
	val requests = (0 until framesInFlight).map { builder.add(12_000, alignment) }

	builder.build(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "AreaEntityBuffer")

	return requests.map { it.get() }
}

private fun entityVertexInput(builder: GraphicsPipelineBuilder, stack: MemoryStack) {
	val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
	vertexBindings.get(0).set(0, 12, VK_VERTEX_INPUT_RATE_INSTANCE)

	val vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack)
	vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
	vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_SINT, 8)

	val vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
	vertexInput.`sType$Default`()
	vertexInput.pVertexBindingDescriptions(vertexBindings)
	vertexInput.pVertexAttributeDescriptions(vertexAttributes)

	builder.ciPipeline.pVertexInputState(vertexInput)
}

class SharedAreaResources(boiler: BoilerInstance, resourcePath: String, framesInFlight: Int, targetImageFormat: Int) {

	private val deviceBuffer: DeviceVkbBuffer
	val entityBuffers = createEntityBuffers(boiler, framesInFlight)
	private val descriptorSetLayout = createDescriptorSetLayout(boiler)
	private val descriptorPool = descriptorSetLayout.createPool(1, 0, "AreaDescriptorPool")
	val descriptorSet = descriptorPool.allocate(1)[0]

	val tilesPipelineLayout: Long
	val entitiesPipelineLayout: Long
	val lowTilesPipeline: Long
	val highTilesPipeline: Long
	val entitiesPipeline: Long

	init {
		val startTime = System.nanoTime()

		stackPush().use { stack ->
			val tilesPushConstants = VkPushConstantRange.calloc(1, stack)
			tilesPushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 52)

			this.tilesPipelineLayout = boiler.pipelines.createLayout(
				tilesPushConstants, "AreaTilesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
			)

			val entitiesPushConstants = VkPushConstantRange.calloc(1, stack)
			entitiesPushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 20)

			this.entitiesPipelineLayout = boiler.pipelines.createLayout(
				entitiesPushConstants, "AreaEntitiesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
			)

			val specializationEntries = VkSpecializationMapEntry.calloc(3, stack)
			specializationEntries.get(0).set(0, 0, 4)
			specializationEntries.get(1).set(1, 4, 4)
			specializationEntries.get(2).set(2, 8, 4)

			val specialization = VkSpecializationInfo.calloc(stack)
			specialization.pMapEntries(specializationEntries)
			this.deviceBuffer = loadMapsAndSprites(boiler, resourcePath, specialization, stack)

			val tilesVertexModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles.vert.spv", "TilesVertexShader"
			)
			val tilesVertexShader = ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, tilesVertexModule, null)
			val entitiesVertexModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/entities.vert.spv", "EntitiesVertexShader"
			)
			val entitiesVertexShader = ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, entitiesVertexModule, specialization)

			val lowTilesFragmentModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles-low.frag.spv", "LowTilesFragmentShader"
			)
			val lowTilesFragmentShader = ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, lowTilesFragmentModule, specialization)

			val lowTiles = simplePipelineBuilder(boiler, stack, targetImageFormat)
			lowTiles.shaderStages(tilesVertexShader, lowTilesFragmentShader)
			lowTiles.noVertexInput()
			lowTiles.ciPipeline.layout(tilesPipelineLayout)
			this.lowTilesPipeline = lowTiles.build("LowTilesPipeline")

			val highTilesFragmentModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/tiles-high.frag.spv", "HighTilesFragmentShader"
			)
			val highTilesFragmentShader = ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, highTilesFragmentModule, specialization)

			val highTiles = simplePipelineBuilder(boiler, stack, targetImageFormat)
			highTiles.shaderStages(tilesVertexShader, highTilesFragmentShader)
			highTiles.noVertexInput()
			highTiles.ciPipeline.layout(tilesPipelineLayout)
			this.highTilesPipeline = highTiles.build("HighTilesPipeline")

			val entitiesFragmentModule = boiler.pipelines.createShaderModule(
				"mardek/renderer/area/entities.frag.spv", "EntitiesFragmentShader"
			)
			val entitiesFragmentShader = ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, entitiesFragmentModule, specialization)

			val entities = simplePipelineBuilder(boiler, stack, targetImageFormat)
			entities.shaderStages(entitiesVertexShader, entitiesFragmentShader)
			entityVertexInput(entities, stack)
			entities.ciPipeline.layout(entitiesPipelineLayout)
			this.entitiesPipeline = entities.build("AreaEntitiesPipeline")

			vkDestroyShaderModule(boiler.vkDevice(), tilesVertexModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), entitiesVertexModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), lowTilesFragmentModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), highTilesFragmentModule, null)
			vkDestroyShaderModule(boiler.vkDevice(), entitiesFragmentModule, null)

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
		entityBuffers[0].buffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), lowTilesPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), highTilesPipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), entitiesPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), tilesPipelineLayout, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), entitiesPipelineLayout, null)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
	}
}
