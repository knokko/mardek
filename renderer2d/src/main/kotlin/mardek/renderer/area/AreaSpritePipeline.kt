package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.memory.callbacks.CallbackUserData
import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.batch.BatchVertexData
import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.pipeline.Vk2dPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.state.util.Rectangle
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32_UINT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.vkCmdPushConstants
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkVertexInputAttributeDescription

class AreaSpritePipeline(context: Vk2dPipelineContext, instance: Vk2dInstance): Vk2dPipeline() {

	private val vkPipelineLayout: Long

	init {
		stackPush().use { stack ->
			val pushConstants = VkPushConstantRange.calloc(1, stack)
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 24)

			this.vkPipelineLayout = instance.boiler.pipelines.createLayout(
				pushConstants, "AreaSpriteLayout",
				instance.bufferDescriptorSetLayout.vkDescriptorSetLayout
			)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(5, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_UINT, 4)
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 8)
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_UINT, 12)
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_SFLOAT, 16)

			val builder = pipelineBuilder(context)
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE)
			builder.simpleShaderStages(
				"AreaSprites", "mardek/renderer/area/",
				"sprite.vert.spv", "sprite.frag.spv"
			)
			builder.ciPipeline.layout(vkPipelineLayout)

			this.vkPipeline = builder.build("Vk2dGlyphPipeline")
		}
	}

	override fun getBytesPerTriangle() = BYTES_PER_TRIANGLE

	override fun getVertexAlignments() = VERTEX_ALIGNMENTS

	override fun recordBatch(
		recorder: CommandRecorder,
		perFrameBuffer: PerFrameBuffer,
		miniBatch: BatchVertexData,
		batch: Vk2dBatch
	) {
		recordNonIndexedBatch(recorder, perFrameBuffer, miniBatch)
	}

	override fun prepareRecording(recorder: CommandRecorder, batch: Vk2dBatch) {
		super.prepareRecording(recorder, batch)

		val spriteBatch = batch as AreaSpriteBatch
		recorder.bindGraphicsDescriptors(vkPipelineLayout, spriteBatch.bundle.fakeImageDescriptorSet)

		val pushConstants = recorder.stack.ints(
			batch.width, batch.height, spriteBatch.scissor.minX, spriteBatch.scissor.minY,
			spriteBatch.scissor.boundX, spriteBatch.scissor.boundY
		)
		vkCmdPushConstants(
			recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, pushConstants
		)
	}

	fun addBatch(
		frame: Vk2dFrame, bundle: Vk2dResourceBundle, scissor: Rectangle
	) = AreaSpriteBatch(this, frame, bundle, scissor)

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
		const val VERTEX_SIZE = 20

		val BYTES_PER_TRIANGLE = intArrayOf(3 * VERTEX_SIZE)
		// Align to 6 vertices because we rely on gl_VertexIndex % 6 being 0 for the first vertex of each quad
		val VERTEX_ALIGNMENTS = intArrayOf(6 * VERTEX_SIZE)
	}
}
