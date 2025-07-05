package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import mardek.renderer.RenderContext
import org.lwjgl.vulkan.VK10.*
import java.lang.Math.toIntExact
import java.nio.IntBuffer

class ColorGridRenderer(
	private val boiler: BoilerInstance,
	private val perFrameBuffer: PerFrameBuffer,
	renderPass: Long,
	descriptorCombiner: DescriptorCombiner,
) {

	private val resources = ColorGridResources(boiler, renderPass, descriptorCombiner)

	fun prepare() {
		resources.prepare(perFrameBuffer)
	}

	fun startBatch(recorder: CommandRecorder) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, resources.graphicsPipeline)
		recorder.bindGraphicsDescriptors(resources.pipelineLayout, resources.descriptorSet)
	}

	fun drawGrid(
		context: RenderContext, minX: Int, minY: Int,
		width: Int, height: Int, table: Int, scale: Int
	): IntBuffer {
		val stages = VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT
		val numTableEntries = width * height
		var numTableInts = numTableEntries / 8
		if (numTableEntries % 8 != 0) numTableInts += 1
		val tableRange = perFrameBuffer.allocate(4L * numTableInts, 4L)
		val pushConstantData = context.recorder.stack.ints(
			context.viewportWidth, context.viewportHeight, minX, minY, width, height,
			toIntExact((tableRange.offset - perFrameBuffer.buffer.offset) / 4), table, scale
		)
		vkCmdPushConstants(
			context.recorder.commandBuffer, resources.pipelineLayout,
			stages, 0, pushConstantData
		)
		vkCmdDraw(
			context.recorder.commandBuffer,
			6, 1, 0, 0
		)
		return tableRange.intBuffer()
	}

	fun endBatch() {}

	fun destroy() {
		resources.destroy(boiler)
	}
}
