package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import org.lwjgl.vulkan.VK10.*

class Kim2Renderer(
	private val boiler: BoilerInstance,
	private val perFrameBuffer: PerFrameBuffer,
	spriteBuffer: VkbBufferRange,
	targetImageFormat: Int,
) {
	private val batches = HashSet<KimBatch>()

	private val resources = Kim2Resources(boiler, targetImageFormat, spriteBuffer)

	fun begin() {
		if (batches.isNotEmpty()) {
			throw IllegalStateException("Invalid call order: did you forget to call end() ?")
		}
	}

	fun startBatch(): KimBatch {
		val batch = KimBatch()
		batches.add(batch)
		return batch
	}

	fun submit(batch: KimBatch, recorder: CommandRecorder, targetImage: VkbImage) {
		if (batch.requests.isEmpty()) return

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, resources.graphicsPipeline)
		recorder.bindGraphicsDescriptors(resources.pipelineLayout, resources.descriptorSet)

		val vertexRange = perFrameBuffer.allocate(KIM2_VERTEX_SIZE.toLong() * batch.requests.size, 4)
		val hostVertexRange = vertexRange.byteBuffer()
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(vertexRange.buffer.vkBuffer),
			recorder.stack.longs(vertexRange.offset)
		)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, recorder.stack.ints(targetImage.width, targetImage.height)
		)

		for (request in batch.requests) {
			if (request.sprite.version != 2) throw IllegalArgumentException("Kim2Renderer only supports kim2 sprites")
			hostVertexRange.putInt(request.x).putInt(request.y).putFloat(request.scale)
			hostVertexRange.putInt(request.sprite.offset).putFloat(request.opacity)
		}
		vkCmdDraw(recorder.commandBuffer, 6, batch.requests.size, 0, 0)
		batch.requests.clear()
	}

	fun end() {
		for (batch in batches) {
			if (batch.requests.isNotEmpty()) throw IllegalStateException(
				"Invalid call order: you must call submit() after adding requests to batches and before calling end()"
			)
		}
		batches.clear()
	}

	fun destroy() {
		resources.destroy(boiler)
		if (batches.isNotEmpty()) throw IllegalStateException(
			"Invalid call order: you must call end() before calling destroy()"
		)
	}
}