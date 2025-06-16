package mardek.renderer.batch

import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.compressor.Kim1Compressor
import com.github.knokko.compressor.Kim2Compressor
import com.github.knokko.ui.renderer.UiRenderInstance
import mardek.content.sprite.KimSprite
import mardek.renderer.createRenderPass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.BufferUtils
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import java.awt.Color
import javax.imageio.ImageIO

class TestBatchRendering {

	@Test
	fun testOrderingAndCorrectnessOfBatchRenderers() {
		val redBuffer = BufferUtils.createByteBuffer(4 * 2 * 2)
		repeat(3) {
			redBuffer.put(255.toByte()).put(0.toByte()).put(0.toByte()).put(255.toByte())
		}
		redBuffer.put(255.toByte()).put(255.toByte()).put(255.toByte()).put(255.toByte())
		redBuffer.flip()
		val redCompressor = Kim1Compressor(redBuffer, 2, 2, 4)

		val orangeBuffer = BufferUtils.createByteBuffer(3 * 2 * 2)
		repeat(3) {
			orangeBuffer.put(255.toByte()).put(255.toByte()).put(0.toByte())
		}
		orangeBuffer.put(255.toByte()).put(255.toByte()).put(255.toByte())
		orangeBuffer.flip()
		val orangeCompressor = Kim1Compressor(orangeBuffer, 2, 2, 3)

		val greenBuffer = BufferUtils.createIntBuffer(2 * 2)
		repeat(3) {
			greenBuffer.put(rgb(0, 255, 0))
		}
		greenBuffer.put(0)
		greenBuffer.flip()
		val greenIntSize = Kim2Compressor.predictIntSize(2, 2, 1)

		val cyanBuffer = BufferUtils.createIntBuffer(2 * 2)
		repeat(3) {
			cyanBuffer.put(rgb(0, 255, 255))
		}
		cyanBuffer.put(0)
		cyanBuffer.flip()
		val cyanIntSize = Kim2Compressor.predictIntSize(2, 2, 2)

		val boiler = BoilerBuilder(
			VK_API_VERSION_1_0, "TestBatchRendering", 1
		).enableDynamicRendering().defaultTimeout(5_000_000_000L).validation().forbidValidationErrors().build()

		val renderPass = createRenderPass(boiler, VK_FORMAT_R8G8B8A8_SRGB)

		val combiner = MemoryCombiner(boiler, "KimMemory")
		val descriptorCombiner = DescriptorCombiner(boiler)
		val kimBuffer = combiner.addMappedBuffer(
			4L * (redCompressor.intSize + orangeCompressor.intSize + greenIntSize + cyanIntSize),
			boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		)
		val perFrameRange = combiner.addMappedBuffer(
			200L, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
		)

		val targetImage = combiner.addImage(ImageBuilder("TargetImage", 10, 10)
			.format(VK_FORMAT_R8G8B8A8_SRGB)
			.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
		)
		val perFrameBuffer = PerFrameBuffer(perFrameRange)

		val uiInstance = UiRenderInstance.withRenderPass(
			boiler, renderPass, 0, combiner, descriptorCombiner, 4
		)
		val kim1Renderer = Kim1Renderer(
			boiler, perFrameBuffer, renderPass, 1,
			descriptorCombiner, combiner
		)
		val kim2Renderer = Kim2Renderer(boiler, perFrameBuffer, renderPass, descriptorCombiner)
		val targetBuffer = combiner.addMappedBuffer(
			4L * targetImage.width * targetImage.height,
			boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_TRANSFER_DST_BIT
		)

		val uiRenderer = uiInstance.createRenderer(perFrameBuffer, 100_000, combiner, descriptorCombiner)

		val memory = combiner.build(false)
		val descriptorPool = descriptorCombiner.build("SharedDescriptorPool")

		fun getSprite(offset: Int, version: Int, compress: (Int) -> Unit): KimSprite {
			compress(offset)
			val sprite = KimSprite(intArrayOf(kimBuffer.intBuffer().get(offset)), version)
			sprite.offset = offset
			return sprite
		}

		val redSprite = getSprite(0, 1) { offset ->
			redCompressor.compress(kimBuffer.child(4L * offset, 4L * redCompressor.intSize).byteBuffer())
		}
		val orangeSprite = getSprite(redCompressor.intSize, 1) { offset ->
			orangeCompressor.compress(kimBuffer.child(4L * offset, 4L * orangeCompressor.intSize).byteBuffer())
		}
		val greenSprite = getSprite(redCompressor.intSize + orangeCompressor.intSize, 2) { offset ->
			Kim2Compressor.compress(greenBuffer, 2, 2, kimBuffer.child(
				4L * offset, 4L * greenIntSize
			).intBuffer(), 1)
		}
		val cyanSprite = getSprite(redCompressor.intSize + orangeCompressor.intSize + greenIntSize, 2) { offset ->
			Kim2Compressor.compress(cyanBuffer, 2, 2, kimBuffer.child(
				4L * offset, 4L * cyanIntSize
			).intBuffer(), 2)
		}

		val framebuffer = boiler.images.createFramebuffer(
			renderPass, targetImage.width, targetImage.height,
			"TestBatchFramebuffer", targetImage.vkImageView
		)

		kim1Renderer.prepare(kimBuffer)
		kim2Renderer.prepare(kimBuffer)
		uiRenderer.prepare()

		perFrameBuffer.startFrame(0)

		val commands = SingleTimeCommands(boiler)
		commands.submit("TestBatchRendering") { recorder ->
			uiInstance.prepare(recorder)
			recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)

			val lateKim1 = kim1Renderer.startBatch()
			val earlyKim1 = kim1Renderer.startBatch()

			val lateKim2 = kim2Renderer.startBatch()
			val earlyKim2 = kim2Renderer.startBatch()

			lateKim1.requests.add(KimRequest(x = 5, y = 3, scale = 1f, sprite = orangeSprite, opacity = 1f))
			lateKim2.requests.add(KimRequest(x = 1, y = 2, scale = 2f, sprite = cyanSprite, opacity = 1f))
			earlyKim2.requests.add(KimRequest(x = 1, y = 7, scale = 1f, sprite = greenSprite, opacity = 1f))
			earlyKim1.requests.add(KimRequest(x = 0, y = 0, scale = 4f, sprite = redSprite, opacity = 1f))

			kim1Renderer.recordBeforeRenderpass(recorder, 0)

			val clearValues = VkClearValue.calloc(1, recorder.stack)
			clearValues.get(0).color().float32(recorder.stack.floats(1f, 0f, 1f, 1f))

			val biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack)
			biRenderPass.`sType$Default`()
			biRenderPass.renderPass(renderPass)
			biRenderPass.framebuffer(framebuffer)
			biRenderPass.renderArea().offset().set(0, 0)
			biRenderPass.renderArea().extent().set(targetImage.width, targetImage.height)
			biRenderPass.pClearValues(clearValues)
			biRenderPass.clearValueCount(1)

			vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
			recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

			uiRenderer.begin(recorder, targetImage)
			uiRenderer.beginBatch()
			uiRenderer.fillColor(3, 0, 10, 10, rgb(100, 100, 100))
			uiRenderer.endBatch()
			kim1Renderer.submit(earlyKim1, recorder, targetImage)
			kim2Renderer.submit(earlyKim2, recorder, targetImage)
			kim1Renderer.submit(lateKim1, recorder, targetImage)

			uiRenderer.beginBatch()
			uiRenderer.fillColor(0, 2, 4, 4, rgb(200, 0, 100))
			kim2Renderer.submit(lateKim2, recorder, targetImage)

			uiRenderer.end()
			kim1Renderer.end()
			kim2Renderer.end()
			vkCmdEndRenderPass(recorder.commandBuffer)

			recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
			recorder.copyImageToBuffer(targetImage, targetBuffer)
		}.awaitCompletion()

		val actualImage = targetBuffer.decodeBufferedImage(targetImage.width, targetImage.height)
		val expectedImage = ImageIO.read(TestBatchRendering::class.java.getResource("expected-batch-rendering.png"))
		assertEquals(expectedImage.width, actualImage.width)
		assertEquals(expectedImage.height, actualImage.height)
		for (y in 0 until actualImage.height) {
			for (x in 0 until actualImage.width) {
				assertEquals(Color(expectedImage.getRGB(x, y), true), Color(actualImage.getRGB(x, y), true))
			}
		}

		commands.destroy()
		uiRenderer.destroy()
		uiInstance.destroy()
		kim2Renderer.destroy()
		kim1Renderer.destroy()
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
		memory.destroy(boiler)
		vkDestroyRenderPass(boiler.vkDevice(), renderPass, null)
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null)
		boiler.destroyInitialObjects()
	}
}
