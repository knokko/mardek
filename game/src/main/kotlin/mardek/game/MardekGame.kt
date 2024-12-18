package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.builders.device.SimpleDeviceSelector
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.assets.Campaign
import mardek.audio.AudioUpdater
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.GameStateManager
import mardek.state.StartupState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
	val startTime = System.nanoTime()
	if (args.contains("self-test1")) {
		selfTest1()
		return
	}

	val mainThread = Thread.currentThread()
	val campaign = CompletableFuture<Campaign>()
	val input = InputManager()
	val state = GameStateManager(input, StartupState(campaign))
	val getBoiler = CompletableFuture<BoilerInstance>()
	val getTargetImageFormat = CompletableFuture<Int>()
	val sharedResources = CompletableFuture<SharedResources>()
	val framesInFlight = 2

	Thread {
		try {
			sharedResources.complete(SharedResources(getBoiler, framesInFlight, getTargetImageFormat))
		} catch (failed: Throwable) {
			sharedResources.completeExceptionally(failed)
		}
		println("Loaded shared resources after ${(System.nanoTime() - startTime) / 1000_000} ms")
	}.start()

	Thread {
		try {
			campaign.complete(Campaign.load("mardek/game/campaign.bin"))
		} catch (failed: Throwable) {
			campaign.completeExceptionally(failed)
			throw failed
		}
		println("Loaded campaign after ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}.start()

	val boiler = try {
		val boilerBuilder = BoilerBuilder(
			VK_API_VERSION_1_2, "MardekKt", 1
		).addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
		if (args.contains("validation")) boilerBuilder.validation().forbidValidationErrors()
		if (args.contains("api-dump")) boilerBuilder.apiDump()
		if (args.contains("integrated")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(
			VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
		))
		if (args.contains("cpu")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(VK_PHYSICAL_DEVICE_TYPE_CPU))
		val boiler = GameRenderer.addBoilerRequirements(boilerBuilder).build()
		getBoiler.complete(boiler)
		boiler
	} catch (failed: Throwable) {
		getBoiler.completeExceptionally(failed)
		throw failed
	}
	getTargetImageFormat.complete(boiler.window().surfaceFormat)
	println("Created boiler after ${(System.nanoTime() - startTime) / 1_000_000} ms")

	Thread {
		val updateLoop = UpdateLoop({ _ ->
			synchronized(state.lock()) {
				state.update(10.milliseconds)
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

	val inputListener = MardekGlfwInput(boiler.window().glfwWindow, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(GameWindow(boiler.window(), framesInFlight, sharedResources, state, startTime))
	println("Started event loop after ${(System.nanoTime() - startTime) / 1000_000} ms")
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
