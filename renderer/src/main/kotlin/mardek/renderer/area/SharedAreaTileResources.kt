package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.descriptors.GrowingDescriptorBank
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPushConstantRange

class SharedAreaTileResources(
	boiler: BoilerInstance,
	stack: MemoryStack
) {
	val pipelineLayout: Long

	val waterImages = boiler.images.create( // TODO Stop sharing this
		16, 16, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
		VK_IMAGE_ASPECT_COLOR_BIT, VK_SAMPLE_COUNT_1_BIT, 1, 5, true, "WaterImages"
	)

	val vertexModule = boiler.pipelines.createShaderModule(
		"mardek/renderer/area/tiles.vert.spv", "TilesVertexShader"
	)
	val fragmentModule = boiler.pipelines.createShaderModule(
		"mardek/renderer/area/tiles.frag.spv", "TilesFragmentShader"
	)
	val waterModule = boiler.pipelines.createShaderModule(
		"mardek/renderer/area/water.frag.spv", "WaterFragmentShader"
	)

	private val descriptorSetLayout: VkbDescriptorSetLayout
	val descriptorBank: GrowingDescriptorBank
	val waterSampler = boiler.images.createSimpleSampler(
		VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST, VK_SAMPLER_ADDRESS_MODE_REPEAT, "WaterSampler"
	)

	init {
		val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
		boiler.descriptors.binding(descriptorBindings, 0,
			VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)
		boiler.descriptors.binding(descriptorBindings, 1,
			VK_DESCRIPTOR_TYPE_SAMPLER,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)
		boiler.descriptors.binding(descriptorBindings, 2,
			VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)

		this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "TilesDescriptorLayout")
		this.descriptorBank = GrowingDescriptorBank(descriptorSetLayout, 0)

		val pushConstants = VkPushConstantRange.calloc(1, stack)
		pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 20)

		this.pipelineLayout = boiler.pipelines.createLayout(
			pushConstants, "TilesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		)
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyShaderModule(boiler.vkDevice(), vertexModule, null)
		vkDestroyShaderModule(boiler.vkDevice(), fragmentModule, null)
		vkDestroyShaderModule(boiler.vkDevice(), waterModule, null)
		descriptorBank.destroy(true)
		descriptorSetLayout.destroy()
		waterImages.destroy(boiler)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		vkDestroySampler(boiler.vkDevice(), waterSampler, null)
	}
}
