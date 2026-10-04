package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline
import mardek.renderer.battle.computeActionBarHeight
import mardek.renderer.menu.determineSectionRenderRegion
import mardek.state.util.Rectangle
import org.lwjgl.vulkan.VK10.vkDestroyFramebuffer

class MardekFramebuffers(
	private val boiler: BoilerInstance, blurPipeline: Vk2dBlurPipeline,
	format: Int, renderPass: Long, fullWidth: Int, fullHeight: Int
) {
	private val fullscreenBlur: Vk2dBlurPipeline.Framebuffer
	private val windowedBlur: Vk2dBlurPipeline.Framebuffer

	private val fullSectionBlur: Vk2dBlurPipeline.Framebuffer
	private val windowedSectionBlur: Vk2dBlurPipeline.Framebuffer

	private val fullActionBarBlur: Vk2dBlurPipeline.Framebuffer
	private val windowedActionBarBlur: Vk2dBlurPipeline.Framebuffer

	val memoryBlock: MemoryBlock

	init {
		val combiner = MemoryCombiner(boiler, "ExtraFramebuffers")

		val windowedWidth = fullWidth - 2 * BORDER_WIDTH
		val windowedHeight = fullHeight - BORDER_WIDTH - FULL_BORDER_HEIGHT

		this.fullscreenBlur = blurPipeline.createFramebuffer(
			combiner, format,
			fullWidth, fullHeight, fullWidth / 4, fullHeight / 4
		)

		val fullSectionRegion = determineSectionRenderRegion(Rectangle(0, 0, fullWidth, fullHeight))
		this.fullSectionBlur = blurPipeline.createFramebuffer(
			combiner, format,
			fullSectionRegion.width, fullSectionRegion.height,
			fullSectionRegion.width, fullSectionRegion.height
		)
		val windowedSectionRegion = determineSectionRenderRegion(
			Rectangle(0, 0, windowedWidth, windowedHeight)
		)

		this.fullActionBarBlur = blurPipeline.createFramebuffer(
			combiner, format,
			fullWidth, computeActionBarHeight(fullHeight),
			fullWidth, computeActionBarHeight(fullHeight),
		)

		this.memoryBlock = combiner.build(false)
		this.fullscreenBlur.createFramebuffer(boiler, renderPass)
		this.fullSectionBlur.createFramebuffer(boiler, renderPass)
		this.fullActionBarBlur.createFramebuffer(boiler, renderPass)

		this.windowedBlur = Vk2dBlurPipeline.Framebuffer(
			fullscreenBlur, 0, 0, windowedWidth, windowedHeight,
			0, windowedWidth / 4, windowedHeight / 4
		)
		this.windowedSectionBlur = Vk2dBlurPipeline.Framebuffer(
			fullSectionBlur, 0, 0, windowedSectionRegion.width, windowedSectionRegion.height,
			0, windowedSectionRegion.width, windowedSectionRegion.height
		)
		this.windowedActionBarBlur = Vk2dBlurPipeline.Framebuffer(
			fullActionBarBlur, 0, 0,
			windowedWidth, computeActionBarHeight(windowedHeight),
			0,
			windowedWidth, computeActionBarHeight(windowedHeight)
		)
	}

	fun getMainBlur(fullscreen: Boolean) = if (fullscreen) fullscreenBlur else windowedBlur

	fun getSectionBlur(fullscreen: Boolean) = if (fullscreen) fullSectionBlur else windowedSectionBlur

	fun getActionBarBlur(fullscreen: Boolean) = if (fullscreen) fullActionBarBlur else windowedActionBarBlur

	fun destroy() {
		vkDestroyFramebuffer(boiler.vkDevice(), fullscreenBlur.sourceFramebuffer, null)
		vkDestroyFramebuffer(boiler.vkDevice(), fullSectionBlur.sourceFramebuffer, null)
		vkDestroyFramebuffer(boiler.vkDevice(), fullActionBarBlur.sourceFramebuffer, null)
		memoryBlock.destroy(boiler)
	}
}
