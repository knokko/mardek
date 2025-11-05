package mardek.renderer.animation

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.batch.MiniBatch
import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.pipeline.Vk2dPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import java.lang.Math.toIntExact

class AnimationPartPipeline(context: Vk2dPipelineContext, vk2d: Vk2dInstance) : Vk2dPipeline(context.printBatchSizes) {

	private val pipelineLayout: Long

	init {
		stackPush().use { stack ->
			this.pipelineLayout = context.boiler.pipelines.createLayout(
				null, "Vk2dImagePipelineLayout",
				vk2d.imageDescriptorSetLayout.vkDescriptorSetLayout,
				vk2d.imageDescriptorSetLayout.vkDescriptorSetLayout,
			)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(4, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32G32_SFLOAT, 16)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32G32B32_UINT, 24)

			val builder = pipelineBuilder(context, stack)
			builder.simpleShaderStages(
				"AnimationPart", "mardek/renderer/animation/",
				"part.vert.spv", "part.frag.spv"
			)
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE)
			builder.ciPipeline.layout(pipelineLayout)

			this.vkPipeline = builder.build("AnimationPartPipeline")
		}
	}

	override fun getBytesPerTriangle() = BYTES_PER_TRIANGLE

	override fun getVertexAlignments() = VERTEX_ALIGNMENTS

	fun addBatch(stage: Vk2dRenderStage, capacity: Int, bundle: Vk2dResourceBundle) = AnimationPartBatch(
		this, stage, capacity, bundle
	)

	override fun recordBatch(
		recorder: CommandRecorder,
		perFrameBuffer: PerFrameBuffer,
		miniBatch: MiniBatch,
		batch: Vk2dBatch,
	) {
		var firstVertex = toIntExact((miniBatch.vertexBuffers[0].offset - perFrameBuffer.buffer.offset) / VERTEX_SIZE)
		val pDescriptorSet = recorder.stack.callocLong(2)
		var index = 0
		var descriptorIndex = 0
		while (index < miniBatch.vertexData[0].position()) {
			val animationBatch = batch as AnimationPartBatch
			pDescriptorSet.put(0, animationBatch.mainDescriptorSets[descriptorIndex])
			pDescriptorSet.put(1, animationBatch.maskDescriptorSets[descriptorIndex])
			vkCmdBindDescriptorSets(
				recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
				pipelineLayout, 0, pDescriptorSet, null
			)
			vkCmdDraw(recorder.commandBuffer, 6, 1, firstVertex, 0)
			firstVertex += 6
			descriptorIndex += 1
			index += 6 * VERTEX_SIZE
		}
	}

	override fun destroy(boiler: BoilerInstance) {
		super.destroy(boiler)
		stackPush().use { stack ->
			vkDestroyPipelineLayout(
				boiler.vkDevice(), pipelineLayout,
				CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			)
		}
	}

	companion object {
		const val VERTEX_SIZE = 36

		val BYTES_PER_TRIANGLE = intArrayOf(3 * VERTEX_SIZE)
		val VERTEX_ALIGNMENTS = intArrayOf(VERTEX_SIZE)
	}
}
