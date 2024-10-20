package mardek.game

import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.window.WindowEventLoop
import com.github.knokko.update.UpdateLoop
import mardek.importer.area.importArea
import mardek.importer.area.importAreaCharacterModel
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.renderer.GameRenderer
import mardek.state.ExitState
import mardek.state.GameStateManager
import mardek.state.InGameState
import mardek.state.area.AreaPosition
import mardek.state.area.AreaState
import mardek.state.character.PlayableCharacter
import mardek.state.story.StoryState
import org.lwjgl.glfw.GLFW.*
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

	val area = importArea("aeropolis_N") // TODO Stop hardcoding this
	val mardekModel = importAreaCharacterModel("mardek_hero")
	val deuganModal = importAreaCharacterModel("deugan_hero")

	val input = InputManager()
	val state = GameStateManager(input, InGameState(
		AreaState(area, AreaPosition(23, 0)),
		StoryState(PlayableCharacter(mardekModel), PlayableCharacter(deuganModal))
	))
	val renderer = GameRenderer(boiler)

	val updateLoop = UpdateLoop({ updateLoop ->
		synchronized(state.lock()) {
			state.update(10.milliseconds)
			if (state.currentState is ExitState) updateLoop.stop()
		}
	}, 10_000_000L)
	val updateThread = Thread(updateLoop)
	updateThread.start()

	val glfwWindow = boiler.window().glfwWindow
	glfwSetKeyCallback(glfwWindow) { _, key, _, action, _ ->
		val inputKey = when (key) {
			GLFW_KEY_LEFT -> InputKey.MoveLeft
			GLFW_KEY_A -> InputKey.MoveLeft
			GLFW_KEY_UP -> InputKey.MoveUp
			GLFW_KEY_W -> InputKey.MoveUp
			GLFW_KEY_RIGHT -> InputKey.MoveRight
			GLFW_KEY_D -> InputKey.MoveRight
			GLFW_KEY_DOWN -> InputKey.MoveDown
			GLFW_KEY_S -> InputKey.MoveDown
			GLFW_KEY_X -> InputKey.Interact
			GLFW_KEY_E -> InputKey.Interact
			GLFW_KEY_Z -> InputKey.Cancel
			GLFW_KEY_Q -> InputKey.Cancel
			else -> null
		}

		if (inputKey != null) {
			input.postEvent(InputKeyEvent(
				inputKey,
				didPress = action == GLFW_PRESS,
				didRelease = action == GLFW_RELEASE,
				didRepeat = action == GLFW_REPEAT
			))
		}
	}

	// TODO Add is-alive logic to the event loop
	val eventLoop = WindowEventLoop()
	eventLoop.addWindow(GameWindow(boiler.window(), state, renderer))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
