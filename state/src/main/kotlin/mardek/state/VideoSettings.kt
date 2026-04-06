package mardek.state

import mardek.state.saves.VIDEO_SETTINGS_FILE
import java.io.File
import java.io.PrintWriter
import java.lang.Integer.parseInt
import java.util.Scanner

/**
 * The (Vulkan) video settings that this engine supports.
 *
 * - The video settings are loaded from [VIDEO_SETTINGS_FILE] when the game is launched, if the file exists.
 * Otherwise, the default video settings are used.
 * - The loaded `VideoSettings` instance is stored in the `MardekWindow.videoSettings` field.
 * - The `VideoSettings can be edited from the "Video Settings" tab of the in-game menu. When the player makes changes,
 * the video settings will be written to [VIDEO_SETTINGS_FILE].
 */
class VideoSettings(

	/**
	 * The preferred `VkPhysicalDevice` index into `vkEnumeratePhysicalDevices`.
	 *
	 * For instance, when `preferredDevice = 1`, the engine will **try** to use the second `VkPhysicalDevice` that is
	 * listed by `vkEnumeratePhysicalDevices`. When `vkEnumerateDevices` lists less than 2 physical devices (or less
	 * than 2 physical devices do not meet the minimum requirements of this game), this field will be reset to its
	 * default value.
	 *
	 * The default value of this field is determined by searching for the first physical device with type
	 * `VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU`. If no such device can be found, one with
	 * `VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU` is used instead. If we can't find such a device either, we just pick the
	 * index of the first physical device (0).
	 *
	 * Note that this setting should not be moved from one computer to the other, since each computer can have a
	 * completely different list of physical devices.
	 */
	var preferredDevice: Int,

	/**
	 * Whether the FPS should be capped to the monitor refresh rate.
	 * - When `true` (by default), the `MardekWindow` will use `VK_PRESENT_MODE_FIFO_KHR`
	 * - When `false`, the `MardekWindow` will try to use `VK_PRESENT_MODE_MAILBOX_KHR` or
	 * `VK_PRESENT_MODE_IMMEDIATE_KHR`. If none of them is supported, this checkbox is disabled, and forced to `true`.
	 */
	var capFps: Boolean,

	/**
	 * Whether the FPS should be displayed on the window title bar
	 */
	var showFps: Boolean,

	/**
	 * The number of frames-in-flight that the `MardekWindow` should use (1 by default). Using more frames-in-flight
	 * typically increases both throughput and latency.
	 *
	 * Since this engine can normally reach 60 fps without much effort, there is normally no need to increase the
	 * throughput, so keeping it at 1 is probably best (to minimize the latency).
	 */
	var framesInFlight: Int,

	/**
	 * Whether rendering should be delayed until the swapchain image has been acquired.
	 * - When this is `true` (by default), the engine will pass a `VkFence` to `vkAcquireNextImageKHR`, and start
	 * rendering the frame after waiting on the fence.
	 * - When this is `false`, this engine will pass a `VkSemaphore` to `vkAcquireNextImageKHR`, and start rendering
	 * the frame right after `vkAcquireNextImageKHR` returns, but delay the submission until that semaphore is
	 * signalled.
	 *
	 * Keeping this at `true` usually gives the lowest perceived latency because the rendering starts a bit later.
	 * When the rendering takes so long that it misses frames, setting this field to `false` may help.
	 */
	var delayRendering: Boolean,
) {

	/**
	 * All the physical devices (from `vkEnumeratePhysicalDevices`) that passed the minimum graphics requirements of
	 * this engine. At the time of writing this:
	 * - The physical device must have a graphics queue family
	 * - The physical device must have a compute queue family
	 * - The physical device must have a queue family that can present to the game window
	 * - The physical device must support BC texture compression
	 *
	 * This is a late-init field that will be initialized by the `MardekBoiler` almost right after the game is launched,
	 * long before the first frame is rendered.
	 */
	lateinit var availableDevices: Array<Any>

	/**
	 * Whether the FPS can be uncapped. When the graphics driver supports `VK_PRESENT_MODE_MAILBOX_KHR` or
	 * `VK_PRESENT_MODE_IMMEDIATE_KHR`, the `MardekBoiler` will set this field to `true` (long before the first
	 * frame is rendered).
	 */
	var canUncapFps = false

	/**
	 * Saves the video settings to disk (to [VIDEO_SETTINGS_FILE] by default, but some unit tests use a different
	 * location).
	 */
	fun save(settingsFile: File = VIDEO_SETTINGS_FILE) {
		try {
			settingsFile.parentFile.mkdirs()
			val writer = PrintWriter(settingsFile)
			writer.println("device=$preferredDevice")
			writer.println("cap-fps=$capFps")
			writer.println("show-fps=$showFps")
			writer.println("frames-in-flight=$framesInFlight")
			writer.println("delay-rendering=$delayRendering")
			writer.flush()
			writer.close()
		} catch (failed: Throwable) {
			failed.printStackTrace()
		}
	}

	companion object {

		/**
		 * Loads the video settings from disk (from [VIDEO_SETTINGS_FILE] by default, but some unit tests use a
		 * different file).
		 */
		fun load(settingsFile: File = VIDEO_SETTINGS_FILE): VideoSettings {
			val settings = VideoSettings(
				preferredDevice = 0,
				capFps = true,
				showFps = false,
				framesInFlight = 1,
				delayRendering = true,
			)

			if (settingsFile.exists()) {
				try {
					val scanner = Scanner(settingsFile)
					while (scanner.hasNextLine()) {
						val nextLine = scanner.nextLine()
						if (nextLine.startsWith("device=")) {
							settings.preferredDevice = parseInt(nextLine.substring("device=".length))
						}
						if (nextLine.startsWith("cap-fps=")) {
							settings.capFps = nextLine.endsWith("true")
						}
						if (nextLine.startsWith("show-fps=")) {
							settings.showFps = nextLine.endsWith("true")
						}
						if (nextLine.startsWith("frames-in-flight=")) {
							settings.framesInFlight = parseInt(nextLine.substring("frames-in-flight=".length))
						}
						if (nextLine.startsWith("delay-rendering")) {
							settings.delayRendering = nextLine.endsWith("true")
						}
					}
					scanner.close()
				} catch (failed: Throwable) {
					failed.printStackTrace()
				}
			}

			return settings
		}
	}
}
