package mardek.game

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.builders.device.SimpleDeviceSelector
import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.content.Content
import mardek.audio.AudioUpdater
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.GameStateManager
import mardek.state.title.TitleScreenState
import org.lwjgl.sdl.SDLInit.SDL_INIT_GAMEPAD
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_CREATOR_STRING
import org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_IDENTIFIER_STRING
import org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_NAME_STRING
import org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_TYPE_STRING
import org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_URL_STRING
import org.lwjgl.sdl.SDLInit.SDL_SetAppMetadataProperty
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_BORDERLESS
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_VULKAN
import org.lwjgl.vulkan.VK10.*
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
	val startTime = System.nanoTime()
	if (args.contains("self-test1")) {
		selfTest1()
		return
	}

	System.setOut(Logger(File("mardek-stdout.txt"), System.out))
	System.setErr(Logger(File("mardek-stderr.txt"), System.err))
	val mainThread = Thread.currentThread()
	val content = CompletableFuture<Content>()
	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState())
	val getBoiler = CompletableFuture<BoilerInstance>()
	val getTargetImageFormat = CompletableFuture<Int>()
	val sharedResources = CompletableFuture<SharedResources>()
	val framesInFlight = 2

	Thread {
		try {
			sharedResources.complete(SharedResources(getBoiler, framesInFlight))
		} catch (failed: Throwable) {
			sharedResources.completeExceptionally(failed)
		}
		println("Loaded shared resources after ${(System.nanoTime() - startTime) / 1000_000} ms")
	}.start()

	val boiler = try {
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

		val boilerBuilder = BoilerBuilder(
			VK_API_VERSION_1_0, "MardekKt", 1
		).addWindow(WindowBuilder(
			800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
			).hideUntilFirstFrame().sdlFlags(SDL_WINDOW_VULKAN or SDL_WINDOW_RESIZABLE or SDL_WINDOW_BORDERLESS)
		).useSDL(SDL_INIT_VIDEO or SDL_INIT_GAMEPAD)
		if (args.contains("validation")) boilerBuilder.validation().forbidValidationErrors()
		if (args.contains("api-dump")) boilerBuilder.apiDump()
		if (args.contains("integrated")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(
			VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
		))
		if (args.contains("cpu")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(VK_PHYSICAL_DEVICE_TYPE_CPU))
		val boiler = GameRenderer.addBoilerRequirements(boilerBuilder).build()
		getBoiler.complete(boiler)
		getTargetImageFormat.complete(boiler.window().surfaceFormat)
		boiler
	} catch (failed: Throwable) {
		getBoiler.completeExceptionally(failed)
		throw failed
	}
	println("Created boiler after ${(System.nanoTime() - startTime) / 1_000_000} ms")

	Thread {
		try {
			content.complete(Content.load("mardek/game/content.bin", Bitser(false)))
		} catch (failed: Throwable) {
			content.completeExceptionally(failed)
			throw failed
		}
		println("Loaded campaign after ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}.start()

	Thread {
		val updateLoop = UpdateLoop({ _ ->
			if (content.isCompletedExceptionally || content.isCancelled) throw RuntimeException("Failed to load campaign")
			if (content.isDone) {
				synchronized(state.lock()) {
					state.update(content.get(), 10.milliseconds)
				}
			}
		}, 10_000_000L)
		val updateThread = Thread(updateLoop)
		updateThread.isDaemon = true
		updateThread.start()

		val audioUpdater = AudioUpdater(state)
		val audioLoop = UpdateLoop({ loop ->
			if (mainThread.isAlive) audioUpdater.update()
			else loop.stop()
		}, 10_000_000L)
		audioLoop.run()
		audioUpdater.destroy()
	}.start()

	val inputListener = MardekSdlInput(boiler.window(), state, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(GameWindow(boiler.window(), framesInFlight, sharedResources, content, state, startTime))
	println("Started event loop after ${(System.nanoTime() - startTime) / 1000_000} ms")
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
