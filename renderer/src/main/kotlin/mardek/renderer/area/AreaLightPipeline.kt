package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.batch.MiniBatch
import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.pipeline.Vk2dPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import mardek.state.util.Rectangle
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VK10.vkCmdPushConstants
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPushConstantRange
import java.lang.Math.toIntExact

class AreaLightPipeline(context: Vk2dPipelineContext, instance: Vk2dInstance): Vk2dPipeline(context.printBatchSizes) {

	private val vkPipelineLayout: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 28)

			this.vkPipelineLayout = instance.boiler.pipelines.createLayout(
				pushConstants, "AreaSpriteLayout",
				instance.bufferDescriptorSetLayout.vkDescriptorSetLayout
			)

			val builder = pipelineBuilder(context, stack)
			builder.noVertexInput()
			builder.simpleShaderStages(
				"AreaLights", "mardek/renderer/area/",
				"light.vert.spv", "light.frag.spv"
			)
			builder.ciPipeline.layout(vkPipelineLayout)

			this.vkPipeline = builder.build("AreaLightPipeline")
		}
	}

	override fun getBytesPerTriangle() = BYTES_PER_TRIANGLE

	override fun getVertexAlignments() = VERTEX_ALIGNMENTS

	override fun recordBatch(
		recorder: CommandRecorder,
		perFrameBuffer: PerFrameBuffer,
		miniBatch: MiniBatch,
		batch: Vk2dBatch
	) {
		val lightBatch = batch as AreaLightBatch
		val numQuads = miniBatch.vertexData[0].position() / QUAD_SIZE
		val byteOffset = toIntExact(miniBatch.vertexBuffers()[0].offset - perFrameBuffer.buffer.offset)
		val pushConstants = recorder.stack.ints(
			batch.width, batch.height, lightBatch.scissor.minX, lightBatch.scissor.minY,
			lightBatch.scissor.boundX, lightBatch.scissor.boundY, byteOffset / QUAD_SIZE,
		)
		vkCmdPushConstants(
			recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, pushConstants
		)
		vkCmdDraw(recorder.commandBuffer, 6 * numQuads, 1, 0, 0)
	}

	override fun prepareRecording(recorder: CommandRecorder, batch: Vk2dBatch) {
		super.prepareRecording(recorder, batch)

		val lightBatch = batch as AreaLightBatch
		recorder.bindGraphicsDescriptors(vkPipelineLayout, lightBatch.perFrameDescriptorSet)
	}

	fun addBatch(
		frame: Vk2dRenderStage, perFrameDescriptorSet: Long, scissor: Rectangle
	) = AreaLightBatch(this, frame, perFrameDescriptorSet, scissor)

	override fun destroy(boiler: BoilerInstance) {
		super.destroy(boiler)
		stackPush().use { stack ->
			vkDestroyPipelineLayout(
				boiler.vkDevice(), vkPipelineLayout,
				CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			)
		}
	}

	companion object {
		const val QUAD_SIZE = 12

		val BYTES_PER_TRIANGLE = intArrayOf(QUAD_SIZE / 2)
		val VERTEX_ALIGNMENTS = intArrayOf(QUAD_SIZE)
	}
}
