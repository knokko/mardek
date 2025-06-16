package mardek.game

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.builders.device.SimpleDeviceSelector
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.content.Content
import mardek.audio.AudioUpdater
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.renderer.SharedResources
import mardek.state.GameStateManager
import mardek.state.title.TitleScreenState
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
		val boilerBuilder = BoilerBuilder(
			VK_API_VERSION_1_0, "MardekKt", 1
		).addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).hideUntilFirstFrame())
		boilerBuilder.printDeviceRejectionInfo()
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
			content.complete(Content.load("mardek/game/content.bin"))
		} catch (failed: Throwable) {
			content.completeExceptionally(failed)
			throw failed
		}
		println("Loaded campaign after ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}.start()

	Thread {
		val updateLoop = UpdateLoop({ _ ->
			if (content.isCompletedExceptionally || content.isCancelled) throw RuntimeException("Failed to load campagin")
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

	val inputListener = MardekGlfwInput(boiler.window().glfwWindow, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(GameWindow(boiler.window(), framesInFlight, sharedResources, content, state, startTime))
	println("Started event loop after ${(System.nanoTime() - startTime) / 1000_000} ms")
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
