package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SwapchainResourceManager
import com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline
import mardek.renderer.MardekFramebuffers

class MardekSwapchainResources(
	private val boiler: BoilerInstance,
	private val blurPipeline: Vk2dBlurPipeline,
	private val swapchainImageFormat: Int,
	private val renderPass: Long,
) : SwapchainResourceManager<MardekFramebuffers, MardekFramebuffers>() {

	override fun createSwapchain(width: Int, height: Int, numImages: Int) = MardekFramebuffers(
		boiler, blurPipeline, swapchainImageFormat, renderPass, width, height
	)

	override fun createImage(swapchain: MardekFramebuffers, swapchainImage: AcquiredImage) = swapchain

	override fun destroySwapchain(swapchain: MardekFramebuffers) = swapchain.destroy()
}
