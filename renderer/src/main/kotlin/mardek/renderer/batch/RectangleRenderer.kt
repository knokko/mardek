package mardek.renderer.batch

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.blue
import com.github.knokko.boiler.utilities.ColorPacker.green
import com.github.knokko.boiler.utilities.ColorPacker.normalize
import com.github.knokko.boiler.utilities.ColorPacker.red
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.unsigned
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX
import org.lwjgl.vulkan.VK10.vkCmdBindPipeline
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import java.lang.Math.clamp
import java.nio.FloatBuffer

private const val RECTANGLE_VERTEX_SIZE = 24

class RectangleRenderer(
	private val boiler: BoilerInstance,
	private val perFrame: PerFrameBuffer,
	renderPass: Long,
) {

	private val pipeline: Long
	private val pipelineLayout: Long

	private var batch: FloatBuffer? = null
	private var width = 0
	private var height = 0

	init {
		stackPush().use { stack ->
			this.pipelineLayout = boiler.pipelines.createLayout(null, "RectangleLayout")

			val vertexBindings = VkVertexInputBindingDescription.calloc(1, stack)
			vertexBindings.get(0).set(0, RECTANGLE_VERTEX_SIZE, VK_VERTEX_INPUT_RATE_VERTEX)

			val vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack)
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0)
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 8)

			val ciVertex = VkPipelineVertexInputStateCreateInfo.calloc(stack)
			ciVertex.`sType$Default`()
			ciVertex.pVertexBindingDescriptions(vertexBindings)
			ciVertex.pVertexAttributeDescriptions(vertexAttributes)

			val builder = GraphicsPipelineBuilder(boiler, stack)
			builder.simpleShaderStages(
				"Rectangle", "mardek/renderer/",
				"rectangle.vert.spv", "rectangle.frag.spv"
			)
			builder.ciPipeline.pVertexInputState(ciVertex)
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
			this.pipeline = builder.build("RectanglePipeline")
		}
	}

	fun beginBatch(recorder: CommandRecorder, targetImage: VkbImage, maxNumRectangles: Int) {
		if (batch != null) throw IllegalStateException("Previous batch was not submitted yet")
		vkCmdBindPipeline(
			recorder.commandBuffer,
			VK_PIPELINE_BIND_POINT_GRAPHICS,
			pipeline
		)
		val buffer = perFrame.allocate(RECTANGLE_VERTEX_SIZE * 6L * maxNumRectangles, 4)
		this.batch = buffer.floatBuffer()
		this.width = targetImage.width
		this.height = targetImage.height
		recorder.bindVertexBuffers(0, buffer)
	}

	fun gradientUnaligned(
		x1: Int, y1: Int, color1: Int,
		x2: Int, y2: Int, color2: Int,
		x3: Int, y3: Int, color3: Int,
		x4: Int, y4: Int, color4: Int
	) {
		val batch = this.batch ?: throw IllegalStateException("No batch is in progress")
		fun putColor(color: Int) {
			batch.put(normalize(red(color)))
			batch.put(normalize(green(color)))
			batch.put(normalize(blue(color)))
			batch.put(normalize(alpha(color)))
		}

		fun putCoordinates(x: Int, y: Int) {
			batch.put(2f * x / width - 1f)
			batch.put(2f * y / height - 1f)
		}

		putCoordinates(x1, y1)
		putColor(color1)
		putCoordinates(x2, y2)
		putColor(color2)
		putCoordinates(x3, y3)
		putColor(color3)
		putCoordinates(x3, y3)
		putColor(color3)
		putCoordinates(x4, y4)
		putColor(color4)
		putCoordinates(x1, y1)
		putColor(color1)
	}

	fun fillUnaligned(
		x1: Int, y1: Int,
		x2: Int, y2: Int,
		x3: Int, y3: Int,
		x4: Int, y4: Int, color: Int,
	) {
		gradientUnaligned(
			x1, y1, color,
			x2, y2, color,
			x3, y3, color,
			x4, y4, color
		)
	}

	private fun clamp(value: Int) = clamp(value.toLong(), 0, 255).toByte()

	fun fill(minX: Int, minY: Int, maxX: Int, maxY: Int, color: Int) {
		gradientUnaligned(
			minX, maxY + 1, color,
			maxX + 1, maxY + 1, color,
			maxX + 1, minY, color,
			minX, minY, color
		)
	}

	fun gradient(minX: Int, minY: Int, maxX: Int, maxY: Int, baseColor: Int, rightColor: Int, upColor: Int) {
		// oppositeColor = baseColor + (rightColor - baseColor) + (upColor - baseColor) =
		//   rightColor + upColor - baseColor
		val oppositeColor = rgba(
			clamp(unsigned(red(rightColor)) + unsigned(red(upColor)) -
					unsigned(red(baseColor))),
			clamp(unsigned(green(rightColor)) + unsigned(green(upColor)) -
					unsigned(green(baseColor))),
			clamp(unsigned(blue(rightColor)) + unsigned(blue(upColor)) -
					unsigned(blue(baseColor))),
			clamp(unsigned(alpha(rightColor)) + unsigned(alpha(upColor)) -
					unsigned(alpha(baseColor))),
		)
		gradientUnaligned(
			minX, maxY + 1, baseColor,
			maxX + 1, maxY + 1, rightColor,
			maxX + 1, minY, oppositeColor,
			minX, minY, upColor
		)
	}

	fun gradientWithBorder(
		minX: Int, minY: Int, maxX: Int, maxY: Int, borderWidth: Int, borderHeight: Int,
		borderColor: Int, baseColor: Int, rightColor: Int, upColor: Int, // TODO Use this more
	) {
		fill(minX, minY, maxX, minY + borderHeight - 1, borderColor)
		fill(minX, maxY + 1 - borderHeight, maxX, maxY, borderColor)
		fill(minX, minY, minX + borderWidth - 1, maxY, borderColor)
		fill(maxX + 1 - borderWidth, minY, maxX, maxY, borderColor)
		gradient(
			minX + borderWidth, minY + borderHeight,
			maxX - borderWidth, maxY - borderHeight,
			baseColor, rightColor, upColor
		)
	}

	fun endBatch(recorder: CommandRecorder) {
		if (batch == null) throw IllegalStateException("No batch was being made")
		vkCmdDraw(
			recorder.commandBuffer,
			batch!!.position() / 6,
			1, 0, 0
		)
		batch = null
	}

	fun destroy() {
		vkDestroyPipeline(boiler.vkDevice(), pipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null)
	}
}
