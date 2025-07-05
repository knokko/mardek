package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SwapchainResourceManager
import mardek.renderer.SharedResources
import org.lwjgl.vulkan.VK10.vkDestroyFramebuffer
import java.util.concurrent.CompletableFuture

class SwapchainResources(
	private val boiler: BoilerInstance, private val getResources: CompletableFuture<SharedResources>
): SwapchainResourceManager<Any, Long>() {

	override fun createImage(swapchain: Any?, swapchainImage: AcquiredImage): Long? {
		return boiler.images.createFramebuffer(
			getResources.join().renderPass, swapchainImage.width(), swapchainImage.height(),
			"SwapchainFrameBuffer", swapchainImage.image().vkImageView
		)
	}

	override fun destroyImage(framebuffer: Long) {
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null)
	}
}
