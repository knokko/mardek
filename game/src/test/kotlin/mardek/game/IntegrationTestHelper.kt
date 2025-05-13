package mardek.game

import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.content.Content
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.ingame.InGameState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.math.abs

private val actualResultsDirectory = File("rendering-test-results/actual")

private fun nearlyEquals(expectedComponent: Int, actualComponent: Int) = abs(expectedComponent - actualComponent) <= 1

private fun nearlyEquals(expected: Color, actual: Color) = nearlyEquals(expected.red, actual.red) &&
		nearlyEquals(expected.green, actual.green) && nearlyEquals(expected.blue, actual.blue) &&
		nearlyEquals(expected.alpha, actual.alpha)

fun TestingInstance.testRendering(
	getResources: CompletableFuture<SharedResources>, state: InGameState,
	width: Int, height: Int, name: String,
	expectedColors: Array<Color>, forbiddenColors: Array<Color>,
) {
	if (!actualResultsDirectory.exists() && !actualResultsDirectory.mkdir()) {
		throw RuntimeException("Failed to create $actualResultsDirectory")
	}

	val renderer = GameRenderer(getResources)

	val targetImage = ImageBuilder("TargetImage($name)", width, height)
		.format(VK_FORMAT_R8G8B8A8_SRGB)
		.setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
		.build(boiler)
	val framebuffer = boiler.images.createFramebuffer(
		getResources.get().renderPass, width, height, "Framebuffer($name)", targetImage.vkImageView
	)
	val destinationBuffer = boiler.buffers.createMapped(
		4L * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "DestinationBuffer($name)"
	)

	val commands = SingleTimeCommands(boiler)
	val getContent = CompletableFuture<Content>()
	getContent.complete(content)
	commands.submit(name) { recorder ->
		recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE)
		renderer.render(getContent, state, recorder, targetImage, framebuffer, 0)
		recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE)
		recorder.copyImageToBuffer(targetImage, destinationBuffer.fullRange())
	}
	commands.destroy()

	val result = boiler.buffers.decodeBufferedImageRGBA(destinationBuffer, 0L, width, height)
	ImageIO.write(result, "PNG", File("$actualResultsDirectory/$name.png"))
	destinationBuffer.destroy(boiler)
	vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
	targetImage.destroy(boiler)

	for (color in expectedColors) {
		assertTrue((0 until result.width).any { x -> (0 until result.height).any { y ->
			nearlyEquals(color, Color(result.getRGB(x, y), true))
		} }, "Expected color $color, but did not find it")
	}

	for (color in forbiddenColors) {
		assertFalse((0 until result.width).any { x -> (0 until result.height).any { y ->
			nearlyEquals(color, Color(result.getRGB(x, y), true))
		} }, "Expected not to find color $color, but found it anyway")
	}
}

fun pressKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = false)

fun repeatKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = true, didRelease = false, didRepeat = true)

fun releaseKeyEvent(key: InputKey) = InputKeyEvent(key, didPress = false, didRelease = true, didRepeat = false)
