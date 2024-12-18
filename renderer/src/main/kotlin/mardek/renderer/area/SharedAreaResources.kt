package mardek.renderer.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.DeviceVkbBuffer
import com.github.knokko.boiler.buffers.MappedVkbBufferRange
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.assets.area.StoredAreaRenderData
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*
import kotlin.collections.ArrayList

private fun loadMapsAndSprites(
	boiler: BoilerInstance, resourcePath: String, areas: MutableMap<UUID, MemoryAreaRenderData>
): DeviceVkbBuffer {
	val input = DataInputStream(BufferedInputStream(SharedAreaResources::class.java.classLoader.getResourceAsStream(resourcePath)!!))

	val spritesSize = input.readInt()

	val deviceBuffer = boiler.buffers.create(
		4L * spritesSize,
		VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Area Buffer"
	)
	val stagingBuffer = boiler.buffers.createMapped(
		deviceBuffer.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "Area Staging Buffer"
	)

	val stagingInts = stagingBuffer.fullMappedRange().intBuffer()
	for (counter in 0 until spritesSize) stagingInts.put(input.readInt())

	@BitStruct(backwardCompatible = false)
	class StoredAreas(
		@BitField(ordering = 0)
		val list: ArrayList<StoredAreaRenderData>
	) {
		@Suppress("unused")
		constructor() : this(ArrayList(0))
	}

	val storedAreas = Bitser(false).deserialize(StoredAreas::class.java, BitInputStream(input))
	for (stored in storedAreas.list) {
		areas[stored.areaID] = MemoryAreaRenderData.pack(stored)
	}

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

class SharedAreaResources(boiler: BoilerInstance, resourcePath: String, framesInFlight: Int, targetImageFormat: Int) {

	val areaMap = mutableMapOf<UUID, MemoryAreaRenderData>()
	private val deviceBuffer: DeviceVkbBuffer
	private val descriptorSetLayout = createDescriptorSetLayout(boiler)
	private val descriptorPool = descriptorSetLayout.createPool(1, 0, "AreaDescriptorPool")
	val descriptorSet = descriptorPool.allocate(1)[0]
	val kimRenderer = KimRenderer(boiler, descriptorSetLayout.vkDescriptorSetLayout, framesInFlight, 1000, targetImageFormat)

	init {
		val startTime = System.nanoTime()

		stackPush().use { stack ->
			this.deviceBuffer = loadMapsAndSprites(boiler, resourcePath, areaMap)

			val descriptorWrites = VkWriteDescriptorSet.calloc(1, stack)
			boiler.descriptors.writeBuffer(
				stack, descriptorWrites, descriptorSet, 0,
				VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, deviceBuffer.fullRange()
			)
			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
		}
		println("Preparing area resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy(boiler: BoilerInstance) {
		deviceBuffer.destroy(boiler)
		descriptorPool.destroy()
		kimRenderer.destroy()
		descriptorSetLayout.destroy()
	}
}
