package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.text.TextInstance
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer
import com.github.knokko.text.font.FontData
import com.github.knokko.text.font.UnicodeFonts
import com.github.knokko.text.vulkan.VulkanTextInstance
import com.github.knokko.text.vulkan.VulkanTextRenderer
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT

class SharedUiResources(private val boiler: BoilerInstance, colorAttachmentFormat: Int, framesInFlight: Int) {

	val textInstance = TextInstance()
	val font = FontData(textInstance, UnicodeFonts.SOURCE)
	val vkTextInstance = VulkanTextInstance(boiler)
	val textPipeline = vkTextInstance.createPipelineWithDynamicRendering(
		0, colorAttachmentFormat, null, null
	)
	val textRenderers: List<VulkanTextRenderer>

	private val textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(framesInFlight, 0, "TextDescriptorPool")
	private val textBuffers: MappedVkbBuffer

	init {
		val builder = SharedMappedBufferBuilder(boiler)
		val glyphQuadRanges = (0 until framesInFlight).map { _ -> Pair(
			builder.add(100_000, 4), builder.add(20_000, 4)
		) }
		this.textBuffers = builder.build(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "TextBuffers")

		val textDescriptorSets = textDescriptorPool.allocate(framesInFlight)
		this.textRenderers = glyphQuadRanges.mapIndexed { index, (getGlyphRange, getQuadRange) ->
			textPipeline.createRenderer(
				font, textDescriptorSets[index], getGlyphRange.get(),
				getQuadRange.get(), 360, 1
			)
		}
	}

	fun destroy() {
		for (renderer in textRenderers) renderer.destroy()
		textDescriptorPool.destroy()
		textBuffers.destroy(boiler)
		textPipeline.destroy()
		vkTextInstance.destroyInitialObjects()
		font.destroy()
		textInstance.destroy()
	}
}
