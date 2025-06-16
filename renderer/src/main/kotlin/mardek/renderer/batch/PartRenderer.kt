package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import mardek.content.animations.ColorTransform
import mardek.content.sprite.BcSprite
import org.joml.Vector2f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPushConstantRange

class PartRenderer(
	private val boiler: BoilerInstance,
	private val bcImages: List<VkbImage>,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
) {

	private val descriptorSetLayout = stackPush().use { stack ->
		val builder = DescriptorSetLayoutBuilder(stack, 1)
		builder.set(0, 0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
		builder.build(boiler, "PartRenderDescriptorLayout")
	}
	private val descriptorSets = descriptorCombiner.addMultiple(descriptorSetLayout, bcImages.size)

	private val pipelineLayout = stackPush().use { stack ->
		val pushConstants = VkPushConstantRange.calloc(2, stack)
		pushConstants[0].set(VK_SHADER_STAGE_VERTEX_BIT, 0, 32)
		pushConstants[1].set(VK_SHADER_STAGE_FRAGMENT_BIT, 32, 8)

		boiler.pipelines.createLayout(pushConstants, "PartRenderPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout)
	}
	private val graphicsPipeline = stackPush().use { stack ->
		val builder = GraphicsPipelineBuilder(boiler, stack)
		builder.simpleShaderStages(
			"PartRender", "mardek/renderer/",
			"part.vert.spv", "part.frag.spv"
		)
		builder.noVertexInput()
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.ciPipeline.layout(pipelineLayout)
		builder.ciPipeline.renderPass(renderPass)
		builder.ciPipeline.subpass(0)
		builder.build("PartRenderPipeline")
	}
	private val sampler = boiler.images.createSimpleSampler(
		VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
		VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "PartRenderSampler"
	)

	fun prepare() {
		stackPush().use { stack ->
			val updater = DescriptorUpdater(stack, 1)
			updater.writeImage(0, 0L, 0, 0L, sampler)

			val imageInfo = updater.descriptorWrites.pImageInfo()!!
			for ((index, descriptorSet) in descriptorSets.withIndex()) {
				updater.descriptorWrites.dstSet(descriptorSet)
				imageInfo.imageView(bcImages[index].vkImageView)
				updater.update(boiler)
			}
		}
	}

	private var recorder: CommandRecorder? = null

	fun startBatch(recorder: CommandRecorder) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		this.recorder = recorder
	}

	fun render(sprite: BcSprite, corners: Array<Vector2f>, color: ColorTransform?) {
		val recorder = this.recorder ?: throw IllegalStateException("You must call startBatch() first")
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSets[sprite.index])

		val rawCorners = recorder.stack.callocFloat(8)
		for (corner in corners) rawCorners.put(corner.x).put(corner.y)
		vkCmdPushConstants(recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, rawCorners.flip())

		val colors = recorder.stack.callocInt(2)
		if (color == null) colors.put(0).put(-1)
		else colors.put(color.addColor).put(color.multiplyColor)

		vkCmdPushConstants(recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 32, colors.flip())
		vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)
	}

	fun endBatch() {
		recorder = null
	}

	fun destroy() {
		vkDestroySampler(boiler.vkDevice(), sampler, null)
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), descriptorSetLayout.vkDescriptorSetLayout, null)
	}
}
