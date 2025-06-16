package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

private fun createDescriptorSetLayout(boiler: BoilerInstance) = stackPush().use { stack ->
	val builder = DescriptorSetLayoutBuilder(stack, 1)
	builder.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)
	builder.build(boiler, "ColorGridDescriptorLayout")
}

class ColorGridResources(
	private val boiler: BoilerInstance,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
) {

	private val descriptorLayout = createDescriptorSetLayout(boiler)
	var descriptorSet = 0L
		private set

	val pipelineLayout: Long
	val graphicsPipeline: Long

	init {
		descriptorCombiner.addSingle(descriptorLayout) { descriptorSet = it }
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, 0, 36)

			this.pipelineLayout = boiler.pipelines.createLayout(
				pushConstants, "ColorGridPipelineLayout", descriptorLayout.vkDescriptorSetLayout
			)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"ColorGrid", "mardek/renderer/",
				"color-grid.vert.spv", "color-grid.frag.spv"
			)
			builder.noVertexInput()
			builder.simpleInputAssembly()
			builder.dynamicViewports(1)
			builder.simpleRasterization(VK_CULL_MODE_NONE)
			builder.noMultisampling()
			builder.noDepthStencil()
			builder.noColorBlending(1)
			builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
			builder.ciPipeline.layout(pipelineLayout)
			builder.ciPipeline.renderPass(renderPass)
			builder.ciPipeline.subpass(0)
			this.graphicsPipeline = builder.build("ColorGridPipeline")
		}
	}

	fun prepare(perFrame: PerFrameBuffer) {
		stackPush().use { stack ->
			val updater = DescriptorUpdater(stack, 1)
			updater.writeStorageBuffer(0, descriptorSet, 0, perFrame.buffer)
			updater.update(boiler)
		}
	}

	fun destroy(boiler: BoilerInstance) {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), descriptorLayout.vkDescriptorSetLayout, null)
	}
}
