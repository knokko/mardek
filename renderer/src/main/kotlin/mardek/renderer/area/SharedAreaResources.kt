package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*

class SharedAreaResources(
	private val boiler: BoilerInstance,
	stack: MemoryStack,
	val targetImageFormat: Int
) {

	val imageSampler = boiler.images.createSimpleSampler(
		VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
		VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "AreaSampler"
	)
	val tiles = SharedAreaTileResources(boiler, stack)
	val entities = SharedAreaEntityResources(boiler, stack, targetImageFormat)

	fun destroy() {
		tiles.destroy(boiler)
		entities.destroy(boiler)
		vkDestroySampler(boiler.vkDevice(), imageSampler, null)
	}
}
