package mardek.renderer.area.water

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
import mardek.state.util.Rectangle
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VK10.vkCmdPushConstants
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPushConstantRange
import java.lang.Math.toIntExact
import kotlin.use

class SimpleWaterPipeline(context: Vk2dPipelineContext, instance: Vk2dInstance): Vk2dPipeline(context.printBatchSizes) {
	private val vkPipelineLayout: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(2, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 4)
			pushConstants.get(1).set(VK_SHADER_STAGE_VERTEX_BIT, 4, 32)

			this.vkPipelineLayout = instance.boiler.pipelines.createLayout(
				pushConstants, "SimpleWaterLayout",
				instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
				instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
			)

			val builder = pipelineBuilder(context, stack)
			builder.noVertexInput()
			builder.simpleShaderStages(
				"SimpleWater", "mardek/renderer/area/water/",
				"simple.vert.spv", "simple.frag.spv"
			)
			builder.ciPipeline.layout(vkPipelineLayout)

			this.vkPipeline = builder.build("SimpleWaterPipeline")
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
		val waterBatch = batch as SimpleWaterBatch
		val numQuads = miniBatch.vertexData[0].position() / QUAD_SIZE
		val byteOffset = toIntExact(miniBatch.vertexBuffers()[0].offset - perFrameBuffer.buffer.offset)
		val fragmentConstants = recorder.stack.ints((System.nanoTime() / 1000L).toInt())
		vkCmdPushConstants(
			recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT,
			0, fragmentConstants
		)
		val vertexConstants = recorder.stack.ints(
			waterBatch.scale, batch.width, batch.height, waterBatch.scissor.minX, waterBatch.scissor.minY,
			waterBatch.scissor.boundX, waterBatch.scissor.boundY, byteOffset / QUAD_SIZE,
		)
		vkCmdPushConstants(
			recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			4, vertexConstants
		)
		vkCmdDraw(recorder.commandBuffer, 6 * numQuads, 1, 0, 0)
	}

	override fun prepareRecording(recorder: CommandRecorder, batch: Vk2dBatch) {
		super.prepareRecording(recorder, batch)

		val waterBatch = batch as SimpleWaterBatch
		recorder.bindGraphicsDescriptors(
			vkPipelineLayout, waterBatch.bundle.fakeImageDescriptorSet,
			waterBatch.perFrameDescriptorSet,
		)
	}

	fun addBatch(
		frame: Vk2dRenderStage, initialCapacity: Int, bundle: Vk2dResourceBundle,
		perFrameDescriptorSet: Long, scissor: Rectangle, scale: Int,
	) = SimpleWaterBatch(this, frame, initialCapacity, bundle, perFrameDescriptorSet, scissor, scale)

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
		const val QUAD_SIZE = 16
		val BYTES_PER_TRIANGLE = intArrayOf(QUAD_SIZE / 2)
		val VERTEX_ALIGNMENTS = intArrayOf(QUAD_SIZE)
	}
}