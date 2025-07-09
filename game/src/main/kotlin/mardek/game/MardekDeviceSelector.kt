package mardek.game

import com.github.knokko.boiler.builders.device.PhysicalDeviceSelector
import mardek.state.VideoSettings
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import kotlin.math.min

class MardekDeviceSelector(private val settings: VideoSettings) : PhysicalDeviceSelector {

	override fun choosePhysicalDevice(
		stack: MemoryStack,
		supportedDevices: Array<VkPhysicalDevice>,
		vkInstance: VkInstance,
	): VkPhysicalDevice? {
		val allDeviceProperties = VkPhysicalDeviceProperties.calloc(supportedDevices.size)
		settings.availableDevices = supportedDevices.mapIndexed{ index, device ->
			val properties = allDeviceProperties.get(index)
			vkGetPhysicalDeviceProperties(device, properties)
			properties
		}.toTypedArray()
		settings.preferredDevice = min(supportedDevices.size - 1, settings.preferredDevice)
		return supportedDevices[settings.preferredDevice]
	}
}
