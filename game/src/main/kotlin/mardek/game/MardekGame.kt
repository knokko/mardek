package mardek.game

import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.builders.device.SimpleDeviceSelector
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.assets.Campaign
import mardek.audio.AudioUpdater
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.state.GameStateManager
import mardek.state.title.TitleScreenState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
	if (args.contains("self-test1")) {
		selfTest1()
		return
	}

	val boilerBuilder = BoilerBuilder(
		VK_API_VERSION_1_2, "MardekKt", 1
	).addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
	if (args.contains("validation")) boilerBuilder.validation().forbidValidationErrors()
	if (args.contains("api-dump")) boilerBuilder.apiDump()
	if (args.contains("integrated")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU))
	if (args.contains("cpu")) boilerBuilder.physicalDeviceSelector(SimpleDeviceSelector(VK_PHYSICAL_DEVICE_TYPE_CPU))
	val boiler = GameRenderer.addBoilerRequirements(boilerBuilder).build()

	val campaign = Campaign.load("mardek/game/campaign.bin")

	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState(campaign))

	val updateLoop = UpdateLoop({ _ ->
		synchronized(state.lock()) {
			state.update(10.milliseconds)
		}
	}, 10_000_000L)
	val updateThread = Thread(updateLoop)
	updateThread.isDaemon = true
	updateThread.start()

	val mainThread = Thread.currentThread()

	Thread {
		val audioUpdater = AudioUpdater(state)
		val audioLoop = UpdateLoop({ loop ->
			if (mainThread.isAlive) {
				synchronized(state.lock()) {
					audioUpdater.update()
				}
			} else loop.stop()
		}, 10_000_000L)
		audioLoop.run()
		audioUpdater.destroy()
	}.start()

	val inputListener = MardekGlfwInput(boiler.window().glfwWindow, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(GameWindow(campaign, boiler.window(), state))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
