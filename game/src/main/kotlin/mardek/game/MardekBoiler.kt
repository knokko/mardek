package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import mardek.state.VideoSettings
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures

fun createBoiler(args: Array<String>, videoSettings: VideoSettings): BoilerInstance {
	assertSdlSuccess(SDL_SetAppMetadataProperty(
		SDL_PROP_APP_METADATA_NAME_STRING, "MARDEK"
	), "SetAppMetadataProperty")
	assertSdlSuccess(SDL_SetAppMetadataProperty(
		SDL_PROP_APP_METADATA_IDENTIFIER_STRING, "com.github.knokko.mardek"
	), "SetAppMetadataProperty")
	assertSdlSuccess(SDL_SetAppMetadataProperty(
		SDL_PROP_APP_METADATA_CREATOR_STRING, "knokko"
	), "SetAppMetadataProperty")
	assertSdlSuccess(SDL_SetAppMetadataProperty(
		SDL_PROP_APP_METADATA_URL_STRING, "https://github.com/knokko/mardek"
	), "SetAppMetadataProperty")
	assertSdlSuccess(SDL_SetAppMetadataProperty(
		SDL_PROP_APP_METADATA_TYPE_STRING, "game"
	), "SetAppMetadataProperty")

	val apiVersion = if (args.contains("gpu-validation")) VK_API_VERSION_1_2 else VK_API_VERSION_1_0
	val boilerBuilder = BoilerBuilder(apiVersion, "MardekKt", 1).addWindow(
		WindowBuilder(
			900, 600, videoSettings.framesInFlight
		)
			.hideFirstFrames(3)
			.sdlFlags(SDL_WINDOW_VULKAN or SDL_WINDOW_RESIZABLE or SDL_WINDOW_BORDERLESS)
			.printSurfaceFormats()
	)
	boilerBuilder.useSDL(SDL_INIT_VIDEO or SDL_INIT_GAMEPAD)
	boilerBuilder.requiredFeatures10("textureCompressionBc", VkPhysicalDeviceFeatures::textureCompressionBC)
	boilerBuilder.featurePicker10 { _, _, toEnable -> toEnable.textureCompressionBC(true) }
	boilerBuilder.physicalDeviceSelector(MardekDeviceSelector(videoSettings))
	boilerBuilder.doNotUseVma()

	if (args.contains("validation")) throw IllegalArgumentException(
		"'validation' argument was removed, use 'basic-validation' or 'gpu-validation' instead"
	)
	if (args.contains("basic-validation") || args.contains("gpu-validation")) {
		boilerBuilder.validation().forbidValidationErrors()
	}
	if (args.contains("basic-validation") && args.contains("gpu-validation")) {
		throw IllegalArgumentException("Use either 'basic-validation' or 'gpu-validation', NOT both")
	}
	if (args.contains("api-dump")) boilerBuilder.apiDump()

	val boiler = boilerBuilder.build()
	if (boiler.window().supportedPresentModes.contains(VK_PRESENT_MODE_MAILBOX_KHR) ||
			boiler.window().supportedPresentModes.contains(VK_PRESENT_MODE_IMMEDIATE_KHR)
	) {
		videoSettings.canUncapFps = true
	} else {
		videoSettings.capFps = true
	}

	return boiler
}
