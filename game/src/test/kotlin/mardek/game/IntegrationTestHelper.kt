package mardek.game

import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple
import com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.text.Vk2dFancyTextStyleCache
import com.github.knokko.vk2d.text.Vk2dTextStyleCache
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.renderer.MardekFramebuffers
import mardek.renderer.PerFrameResources
import mardek.state.GameState
import mardek.state.GameStateManager
import mardek.state.SoundQueue
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.fail
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memCopy
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.io.File
import java.nio.ByteOrder
import kotlin.time.Duration

internal val actualResultsDirectory = File("rendering-test-results/actual")

fun TestingInstance.testRendering(
	state: GameState,
	width: Int, height: Int, name: String,
	expectedColors: Array<Color>, forbiddenColors: Array<Color>,
	soundQueue: SoundQueue? = null,
	saves: SavesFolderManager = SavesFolderManager(),
) {
	val stateManager = GameStateManager(InputManager(), state, saves)
	testRendering(stateManager, width, height, name, expectedColors, forbiddenColors)
	if (soundQueue != null) {
		var nextSound = stateManager.soundQueue.take()
		while (nextSound != null) {
			soundQueue.insert(nextSound)
			nextSound = stateManager.soundQueue.take()
		}
	}
}

fun TestingInstance.testRendering(
	state: GameStateManager,
	width: Int, height: Int, name: String,
	expectedColors: Array<Color>, forbiddenColors: Array<Color>,
) {
	val standardExpectedColors = arrayOf(
		Color(0, 0, 0, 0), // Window borders must be fully transparent
		Color(73, 59, 50), // Title bar color
		Color(132, 105, 83), // Title bar icon color
	)
	val allColors = standardExpectedColors + expectedColors + forbiddenColors

	val combiner = MemoryCombiner(boiler, "TestHelper$name")
	val descriptorCombiner = DescriptorCombiner(boiler)
	val perFrameDescriptorSet = descriptorCombiner.addMultiple(vk2d.bufferDescriptorSetLayout, 1)
	val checkDescriptorSet = descriptorCombiner.addMultiple(checkDescriptorLayout, 1)
	val perFrameAlignment = leastCommonMultiple(setOf(
		4, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()
	))
	val perFrameBuffer = PerFrameBuffer(combiner.addMappedBuffer(
		2000_000L, perFrameAlignment,
		VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
	))
	val targetImage = combiner.addImage(ImageBuilder("TargetImage($name)", width, height)
		.format(VK_FORMAT_R8G8B8A8_SRGB)
		.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT), 1f
	)

	val storageAlignment = leastCommonMultiple(
		4L, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()
	)
	val checkImageBuffer = combiner.addBuffer(
		4L * width * height, storageAlignment,
		VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 1f
	)
	val readbackImageBuffer = combiner.addMappedBuffer(
		4L * width * height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
	)
	val colorsBuffer = combiner.addMappedDeviceLocalBuffer(
		4L * allColors.size, storageAlignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 0.5f
	)
	val colorPositionsBuffer = combiner.addMappedBuffer(
		4L * targetImage.height * allColors.size, storageAlignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
	)

	val perFrameResources = PerFrameResources(
		areaBlurDescriptors = renderManager.pipelines.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
		sectionsBlurDescriptors = renderManager.pipelines.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
		actionBarBlurDescriptors = renderManager.pipelines.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
	)

	val memory = combiner.build(false)
	val descriptorPool = descriptorCombiner.build("TestHelper$name")
	val textStyleCache = Vk2dTextStyleCache(perFrameBuffer, perFrameDescriptorSet[0])
	val fancyTextStyleCache = Vk2dFancyTextStyleCache(perFrameBuffer, perFrameDescriptorSet[0])

	val mainFramebuffer = boiler.images.createFramebuffer(
		pipelineContext.vkRenderPass, width, height,
		"Framebuffer($name)", targetImage.vkImageView
	)
	val framebuffers = MardekFramebuffers(
		boiler, renderManager.pipelines.blur,
		VK_FORMAT_R8G8B8A8_SRGB, pipelineContext.vkRenderPass, width, height
	)

	stackPush().use { stack ->
		val updater = DescriptorUpdater(stack, 4)
		updater.writeStorageBuffer(0, perFrameDescriptorSet[0], 0, perFrameBuffer.buffer,)
		updater.writeStorageBuffer(1, checkDescriptorSet[0], 0, checkImageBuffer)
		updater.writeStorageBuffer(2, checkDescriptorSet[0], 1, colorsBuffer)
		updater.writeStorageBuffer(3, checkDescriptorSet[0], 2, colorPositionsBuffer)
		updater.update(boiler)
	}

	val imageViewToFramebuffer = hashMapOf(
		Pair(targetImage.vkImageView, mainFramebuffer)
	)

	perFrameBuffer.startFrame(0)

	val writeColors = colorsBuffer.intBuffer()
	for (color in allColors) writeColors.put(rgba(color.red, color.green, color.blue, color.alpha))

	SingleTimeCommands.submit(boiler, name) { recorder ->
		val frame = Vk2dSwapchainFrame(
			targetImage, perFrameBuffer, pipelineContext.vkRenderPass, imageViewToFramebuffer
		)
		recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
		renderManager.renderFrame(
			state, frame, textStyleCache, fancyTextStyleCache, perFrameDescriptorSet[0],
			framebuffers, perFrameResources, 123, Duration.ZERO,
		)
		frame.record(recorder)

		recorder.transitionLayout(
			targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE,
			ResourceUsage.TRANSFER_SOURCE
		)
		recorder.copyImageToBuffer(targetImage, checkImageBuffer)
		recorder.copyImageToBuffer(targetImage, readbackImageBuffer)
		recorder.bufferBarrier(
			readbackImageBuffer,
			ResourceUsage.TRANSFER_DEST,
			ResourceUsage.HOST_READ
		)

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, checkPipeline)
		recorder.bindComputeDescriptors(checkPipelineLayout, checkDescriptorSet[0])
		vkCmdPushConstants(
			recorder.commandBuffer, checkPipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0, recorder.stack.ints(
				width, height, allColors.size, if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) 1 else 0
			)
		)
		vkCmdDispatch(
			recorder.commandBuffer,
			nextMultipleOf(height, 64) / 64,
			1, 1
		)
		recorder.bufferBarrier(
			colorPositionsBuffer,
			ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
			ResourceUsage.HOST_READ
		)
	}.destroy()

	val cpuImageBuffer = memCalloc(readbackImageBuffer.size.toInt())
	memCopy(readbackImageBuffer.byteBuffer(), cpuImageBuffer)
	Thread {
		stbi_write_png(
			File("$actualResultsDirectory/$name.png").absolutePath,
			targetImage.width, targetImage.height, 4,
			cpuImageBuffer, 0
		)
		memFree(cpuImageBuffer)
	}.start()

	val colorPositions = colorPositionsBuffer.intBuffer()
	for ((index, color) in allColors.withIndex()) {
		var foundIt = false
		for (y in 0 until targetImage.height) {
			val x = colorPositions.get()
			if (x != -1) {
				foundIt = true
				if (index >= expectedColors.size + standardExpectedColors.size) {
					fail<Unit>("Expected not to find $color, but found it at ($x, $y)")
				}
			}
		}

		if (index < expectedColors.size + standardExpectedColors.size && !foundIt) {
			fail<Unit>("Expected $color, but did not find it")
		}
	}

	vkDestroyFramebuffer(boiler.vkDevice(), mainFramebuffer, null)
	textStyleCache.destroy()
	fancyTextStyleCache.destroy()
	vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null)
	memory.destroy(boiler)
	framebuffers.destroy()
}

fun pressKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = false)

fun repeatKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = true)

fun releaseKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = false, didRelease = true, didRepeat = false)
