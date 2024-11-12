package mardek.game

import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.assets.GameAssets
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.state.GameStateManager
import mardek.state.InGameState
import mardek.state.area.AreaPosition
import mardek.state.area.AreaState
import mardek.state.story.StoryState
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2
import kotlin.time.Duration.Companion.milliseconds

fun main() {
	val boilerBuilder = BoilerBuilder(
		VK_API_VERSION_1_2, "MardekKt", 1
	)
		.validation()
		.forbidValidationErrors()
		.addWindow(WindowBuilder(800, 600, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
	val boiler = GameRenderer.addBoilerRequirements(boilerBuilder).build()

	val assets = GameAssets.load("mardek/game/areas.bin")
	val area = assets.areas.find { it.properties.rawName == "aeropolis_N" }!!

	val input = InputManager()
	//val state = GameStateManager(input, TitleScreenState())
	val state = GameStateManager(input, InGameState(
		AreaState(area, AreaPosition(23, 40)),
		StoryState(assets.playableCharacters[0], assets.playableCharacters[1])
	))

	val updateLoop = UpdateLoop({ _ ->
		synchronized(state.lock()) {
			state.update(10.milliseconds)
		}
	}, 10_000_000L)
	val updateThread = Thread(updateLoop)
	updateThread.isDaemon = true
	updateThread.start()

	val inputListener = MardekGlfwInput(boiler.window().glfwWindow, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(GameWindow(boiler.window(), state))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
