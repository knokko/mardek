package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
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
	val descriptorSet: Long

	val vertexModule = boiler.pipelines.createShaderModule(
		"mardek/renderer/area/tiles.vert.spv", "TilesVertexShader"
	)
	val fragmentModule = boiler.pipelines.createShaderModule(
		"mardek/renderer/area/tiles.frag.spv", "TilesFragmentShader"
	)

	private val descriptorSetLayout: VkbDescriptorSetLayout
	private val descriptorPool: HomogeneousDescriptorPool

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
		this.descriptorPool = descriptorSetLayout.createPool(1, 0, "TilesDescriptorPool")
		this.descriptorSet = descriptorPool.allocate(1)[0]

		val pushConstants = VkPushConstantRange.calloc(1, stack)
		pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 24)

		this.pipelineLayout = boiler.pipelines.createLayout(
			pushConstants, "TilesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		)
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyShaderModule(boiler.vkDevice(), vertexModule, null)
		vkDestroyShaderModule(boiler.vkDevice(), fragmentModule, null)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
	}
}
