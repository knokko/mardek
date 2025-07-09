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
	format: Int, renderPass: Long, width: Int, height: Int
) {
	val blur: Vk2dBlurPipeline.Framebuffer
	val sectionBlur: Vk2dBlurPipeline.Framebuffer
	val actionBarBlur: Vk2dBlurPipeline.Framebuffer
	val memoryBlock: MemoryBlock

	init {
		val combiner = MemoryCombiner(boiler, "ExtraFramebuffers")
		val blurWidth = width - 2 * BORDER_WIDTH
		val blurHeight = height - BORDER_WIDTH - FULL_BORDER_HEIGHT
		val bufferWidth = blurWidth / 4
		val bufferHeight = blurHeight / 4

		this.blur = blurPipeline.createFramebuffer(
			combiner, format,
			blurWidth, blurHeight, bufferWidth, bufferHeight
		)

		val sectionRegion = determineSectionRenderRegion(Rectangle(0, 0, blurWidth, blurHeight))
		this.sectionBlur = blurPipeline.createFramebuffer(
			combiner, format,
			sectionRegion.width, sectionRegion.height,
			sectionRegion.width, sectionRegion.height
		)
		val actionRegion = Rectangle(0, 0, blurWidth, computeActionBarHeight(blurHeight))
		this.actionBarBlur = blurPipeline.createFramebuffer(
			combiner, format,
			actionRegion.width, actionRegion.height,
			actionRegion.width, actionRegion.height,
		)
		this.memoryBlock = combiner.build(false)
		this.blur.createFramebuffer(boiler, renderPass)
		this.sectionBlur.createFramebuffer(boiler, renderPass)
		this.actionBarBlur.createFramebuffer(boiler, renderPass)
	}

	fun destroy() {
		vkDestroyFramebuffer(boiler.vkDevice(), blur.sourceFramebuffer, null)
		vkDestroyFramebuffer(boiler.vkDevice(), sectionBlur.sourceFramebuffer, null)
		vkDestroyFramebuffer(boiler.vkDevice(), actionBarBlur.sourceFramebuffer, null)
		memoryBlock.destroy(boiler)
	}
}
