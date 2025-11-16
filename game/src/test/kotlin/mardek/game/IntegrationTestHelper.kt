package mardek.game

import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.descriptors.DescriptorUpdater
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame
import com.github.knokko.vk2d.text.Vk2dTextBuffer
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.renderer.MardekFramebuffers
import mardek.renderer.PerFrameResources
import mardek.state.GameState
import mardek.state.GameStateManager
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

internal val actualResultsDirectory = File("rendering-test-results/actual")

private fun nearlyEquals(expectedComponent: Int, actualComponent: Int) = abs(expectedComponent - actualComponent) <= 2

private fun nearlyEquals(expected: Color, actual: Color) = nearlyEquals(expected.red, actual.red) &&
		nearlyEquals(expected.green, actual.green) && nearlyEquals(expected.blue, actual.blue) &&
		nearlyEquals(expected.alpha, actual.alpha)

fun TestingInstance.testRendering(
	state: GameState,
	width: Int, height: Int, name: String,
	expectedColors: Array<Color>, forbiddenColors: Array<Color>,
) = testRendering(
	GameStateManager(InputManager(), state, SavesFolderManager()),
	 width, height, name, expectedColors, forbiddenColors
)

fun TestingInstance.testRendering(
	state: GameStateManager,
	width: Int, height: Int, name: String,
	expectedColors: Array<Color>, forbiddenColors: Array<Color>,
) {
	val combiner = MemoryCombiner(boiler, "TestHelper$name")
	val descriptorCombiner = DescriptorCombiner(boiler)
	val perFrameDescriptorSet = descriptorCombiner.addMultiple(vk2d.bufferDescriptorSetLayout, 1)
	val perFrameAlignment = leastCommonMultiple(setOf(
		4, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()
	))
	val perFrameBuffer = PerFrameBuffer(combiner.addMappedBuffer(
		2000_000L, perFrameAlignment,
		VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
	))
	val textBuffer = Vk2dTextBuffer(vk2d, combiner, descriptorCombiner, 1)
	val targetImage = combiner.addImage(ImageBuilder("TargetImage($name)", width, height)
		.format(VK_FORMAT_R8G8B8A8_SRGB)
		.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT), 1f
	)
	val destinationBuffer = combiner.addMappedBuffer(
		4L * width * height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
	)

	val perFrameResources = PerFrameResources(
		areaBlurDescriptors = renderManager.pipelines.base.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
		sectionsBlurDescriptors = renderManager.pipelines.base.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
		actionBarBlurDescriptors = renderManager.pipelines.base.blur.claimResources(
			1, vk2d, descriptorCombiner
		)[0],
	)

	val memory = combiner.build(false)
	val descriptorPool = descriptorCombiner.build("TestHelper$name")

	val mainFramebuffer = boiler.images.createFramebuffer(
		pipelineContext.vkRenderPass, width, height,
		"Framebuffer($name)", targetImage.vkImageView
	)
	val framebuffers = MardekFramebuffers(
		boiler, renderManager.pipelines.base.blur,
		VK_FORMAT_R8G8B8A8_SRGB, pipelineContext.vkRenderPass, width, height
	)

	stackPush().use { stack ->
		val updater = DescriptorUpdater(stack, 1)
		updater.writeStorageBuffer(
			0, perFrameDescriptorSet[0],
			0, perFrameBuffer.buffer,
		)
		updater.update(boiler)
	}

	val imageViewToFramebuffer = hashMapOf(
		Pair(targetImage.vkImageView, mainFramebuffer)
	)

	perFrameBuffer.startFrame(0)
	textBuffer.initializeDescriptorSets()

	SingleTimeCommands.submit(boiler, name) { recorder ->
		val frame = Vk2dSwapchainFrame(
			targetImage, perFrameBuffer, pipelineContext.vkRenderPass, imageViewToFramebuffer
		)
		frame.stages.add(textBuffer)
		recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
		renderManager.renderFrame(
			state, frame, recorder, textBuffer, perFrameDescriptorSet[0],
			framebuffers, perFrameResources, 123
		)
		frame.record(recorder)
		recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
		recorder.copyImageToBuffer(targetImage, destinationBuffer)
		recorder.bufferBarrier(destinationBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ)
	}.destroy()

	val result = destinationBuffer.decodeBufferedImage(width, height)
	ImageIO.write(result, "PNG", File("$actualResultsDirectory/$name.png"))
	vkDestroyFramebuffer(boiler.vkDevice(), mainFramebuffer, null)
	vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null)
	memory.destroy(boiler)
	framebuffers.destroy()

	val standardExpectedColors = arrayOf(
		Color(0, 0, 0, 0), // Window borders must be fully transparent
		Color(73, 59, 50), // Title bar color
		Color(132, 105, 83), // Title bar icon color
	)
	for (color in expectedColors + standardExpectedColors) {
		assertTrue((0 until result.width).any { x -> (0 until result.height).any { y ->
			nearlyEquals(color, Color(result.getRGB(x, y), true))
		} }, "Expected color $color, but did not find it")
	}

	for (color in forbiddenColors) {
		for (x in 0 until result.width) {
			for (y in 0 until result.height) {
				if (nearlyEquals(color, Color(result.getRGB(x, y), true))) {
					fail("Expected not to find color $color, but found it at ($x, $y)")
				}
			}
		}
	}
}

fun pressKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = false)

fun repeatKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = true)

fun releaseKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = false, didRelease = true, didRepeat = false)
