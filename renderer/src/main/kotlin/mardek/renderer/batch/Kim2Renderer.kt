package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.VkbBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import mardek.renderer.RenderContext
import org.lwjgl.vulkan.VK10.*

class Kim2Renderer(
	private val boiler: BoilerInstance,
	private val perFrameBuffer: PerFrameBuffer,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
) {
	private val batches = HashSet<KimBatch>()

	private val resources = Kim2Resources(boiler, renderPass, descriptorCombiner)

	fun prepare(spriteBuffer: VkbBuffer) {
		resources.prepare(spriteBuffer)
	}

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

	fun submit(batch: KimBatch, context: RenderContext) {
		submit(batch, context.recorder, context.viewportWidth, context.viewportHeight)
	}

	fun submit(batch: KimBatch, recorder: CommandRecorder, viewportWidth: Int, viewportHeight: Int) {
		if (batch.requests.isEmpty()) return

		vkCmdBindPipeline(
			recorder.commandBuffer,
			VK_PIPELINE_BIND_POINT_GRAPHICS,
			resources.graphicsPipeline
		)
		recorder.bindGraphicsDescriptors(resources.pipelineLayout, resources.descriptorSet)

		val vertexRange = perFrameBuffer.allocate(KIM2_VERTEX_SIZE.toLong() * batch.requests.size, 4)
		val hostVertexRange = vertexRange.byteBuffer()
		recorder.bindVertexBuffers(0, vertexRange)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, recorder.stack.ints(viewportWidth, viewportHeight)
		)

		for (request in batch.requests) {
			if (request.sprite.version != 2) throw IllegalArgumentException("Kim2Renderer only supports kim2 sprites")
			hostVertexRange.putInt(request.x).putInt(request.y).putFloat(request.scale)
			hostVertexRange.putInt(request.sprite.offset).putFloat(request.opacity)
			if (request.rotation != 0f) throw UnsupportedOperationException("Kim2Renderer doesn't support rotations")
		}
		vkCmdDraw(
			recorder.commandBuffer, 6,
			batch.requests.size, 0, 0
		)
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
