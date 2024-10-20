package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

class SharedAreaEntityResources(
	boiler: BoilerInstance,
	stack: MemoryStack,
	targetImageFormat: Int
) {

	val descriptorSetLayout: VkbDescriptorSetLayout
	val descriptorPool: HomogeneousDescriptorPool
	val descriptorSet: Long

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(2, stack)
		boiler.descriptors.binding(descriptorBindings, 0, VK_DESCRIPTOR_TYPE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
		boiler.descriptors.binding(descriptorBindings, 1, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, VK_SHADER_STAGE_FRAGMENT_BIT)

		this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "AreaEntityDSLayout")
		this.descriptorPool = descriptorSetLayout.createPool(1, 0, "AreaEntityDescriptorPool")
		this.descriptorSet = descriptorPool.allocate(1)[0]

		val pushConstants = VkPushConstantRange.calloc(1, stack)
		pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 20)

		this.pipelineLayout = boiler.pipelines.createLayout(
			pushConstants, "AreaEntityPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		)

		val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
		vertexBindings.get(0).set(0, 12, VK_VERTEX_INPUT_RATE_INSTANCE)

		val vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack)
		vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SINT, 0)
		vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_SINT, 8)

		val vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
		vertexInput.`sType$Default`()
		vertexInput.pVertexBindingDescriptions(vertexBindings)
		vertexInput.pVertexAttributeDescriptions(vertexAttributes)

		val builder = GraphicsPipelineBuilder(boiler, stack)
		builder.simpleShaderStages(
			"AreaEntities", "mardek/renderer/area/entities.vert.spv",
			"mardek/renderer/area/entities.frag.spv"
		)
		builder.ciPipeline.pVertexInputState(vertexInput)
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.ciPipeline.layout(pipelineLayout)
		builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
		this.graphicsPipeline = builder.build("AreaEntityPipeline")
	}

	fun destroy(boiler: BoilerInstance) {
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
	}
}
