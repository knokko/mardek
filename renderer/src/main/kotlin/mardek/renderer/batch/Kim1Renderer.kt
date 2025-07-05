package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.VkbBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.renderer.RenderContext
import org.joml.Math.toRadians
import org.lwjgl.vulkan.VK10.*
import java.lang.Math.toIntExact

class Kim1Renderer(
	private val boiler: BoilerInstance,
	private val perFrameBuffer: PerFrameBuffer,
	renderPass: Long,
	framesInFlight: Int,
	descriptorCombiner: DescriptorCombiner,
	persistentCombiner: MemoryCombiner,
) {
	private val batches = HashSet<KimBatch>()

	private val resources = Kim1Resources(
		boiler, framesInFlight, renderPass, descriptorCombiner, persistentCombiner
	)

	fun prepare(spriteBuffer: VkbBuffer) {
		resources.prepare(perFrameBuffer, spriteBuffer)
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

	/**
	 * Maps `kimSprite.offset`s to the offset into `middleBuffer` that contains the decompressed `kimSprite`
	 */
	private val offsetMap = mutableMapOf<Int, Int>()

	/**
	 * Maps pairs `(width, height)` to a list containing all requests in this frame with the given size
	 */
	private val sizeMap = mutableMapOf<Pair<Int, Int>, MutableList<KimRequest>>()

	fun recordBeforeRenderpass(recorder: CommandRecorder, frameIndex: Int) {
		val allRequests = batches.flatMap { it.requests }
		if (allRequests.isEmpty()) return
		if (offsetMap.isNotEmpty() || sizeMap.isNotEmpty()) {
			throw IllegalStateException("Invalid call order: you must call this exactly once before each call to end()")
		}

		val readUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
		val writeUsage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT)
		recorder.bufferBarrier(resources.middleBuffer, readUsage, writeUsage)

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, resources.computePipeline)
		recorder.bindComputeDescriptors(resources.computeLayout, resources.computeDescriptorSets[frameIndex])

		for (request in allRequests) {
			if (request.sprite.version != 1) throw IllegalArgumentException("Kim1Renderer only supports kim1 sprites")
			val size = Pair(request.sprite.width, request.sprite.height)
			sizeMap.computeIfAbsent(size) { ArrayList() }.add(request)
		}

		val offsetRange = perFrameBuffer.allocate(4L * sizeMap.values.sumOf { it.size }, 4)
		val baseOffset = toIntExact((offsetRange.offset - perFrameBuffer.buffer.offset) / 4)
		val offsetIntBuffer = offsetRange.intBuffer()
		var nextResultOffset = 0
		for ((size, requests) in sizeMap) {
			vkCmdPushConstants(
				recorder.commandBuffer, resources.computeLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0,
				recorder.stack.ints(size.first, size.second, nextResultOffset, offsetIntBuffer.position() + baseOffset)
			)

			var counter = 0
			for (request in requests) {
				if (offsetMap.containsKey(request.sprite.offset)) continue
				offsetMap[request.sprite.offset] = nextResultOffset
				nextResultOffset += size.first * size.second
				offsetIntBuffer.put(request.sprite.offset)
				counter += 1
			}
			vkCmdDispatch(recorder.commandBuffer, size.first, size.second, counter)
		}

		recorder.bufferBarrier(resources.middleBuffer, writeUsage, readUsage)
	}

	fun submit(batch: KimBatch, context: RenderContext) {
		submit(batch, context.recorder, context.viewportWidth, context.viewportHeight)
	}

	fun submit(batch: KimBatch, recorder: CommandRecorder, viewportWidth: Int, viewportHeight: Int) {
		if (batch.requests.isEmpty()) return
		if (offsetMap.isEmpty()) throw IllegalStateException(
			"Invalid call order: you must call submit() between recordBeforeRenderpass() and end()"
		)

		vkCmdBindPipeline(
			recorder.commandBuffer,
			VK_PIPELINE_BIND_POINT_GRAPHICS,
			resources.graphicsPipeline
		)
		recorder.bindGraphicsDescriptors(resources.graphicsLayout, resources.graphicsDescriptorSet)

		val vertexRange = perFrameBuffer.allocate(KIM1_VERTEX_SIZE.toLong() * batch.requests.size, 4)
		val hostVertexRange = vertexRange.byteBuffer()
		recorder.bindVertexBuffers(0, vertexRange)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.graphicsLayout, VK_SHADER_STAGE_VERTEX_BIT,
			0, recorder.stack.ints(viewportWidth, viewportHeight)
		)

		for (request in batch.requests) {
			if (request.sprite.version != 1) throw IllegalArgumentException("Kim1Renderer only supports kim1 sprites")
			hostVertexRange.putInt(request.x).putInt(request.y)
			hostVertexRange.putInt(request.sprite.width).putInt(request.sprite.height).putFloat(request.scale)
			hostVertexRange.putInt(offsetMap[request.sprite.offset]!!).putFloat(request.opacity)
			hostVertexRange.putFloat(toRadians(request.rotation))
			hostVertexRange.putInt(request.blinkColor)
			hostVertexRange.putFloat(request.blinkIntensity)
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
		offsetMap.clear()
		sizeMap.clear()
	}

	fun destroy() {
		resources.destroy(boiler)

		if (batches.isNotEmpty() || offsetMap.isNotEmpty()) throw IllegalStateException(
			"Invalid call order: you must call end() before calling destroy()"
		)
	}
}
