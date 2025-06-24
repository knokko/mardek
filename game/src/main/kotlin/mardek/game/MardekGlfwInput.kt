package mardek.game

import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.system.MemoryStack.stackPush
import kotlin.math.roundToInt

class MardekGlfwInput(private val glfwWindow: Long, private val input: InputManager) {

	private val joysticks = mutableSetOf<Int>()

	private var wasMovingLeft = false
	private var wasMovingDown = false
	private var wasMovingRight = false
	private var wasMovingUp = false

	fun register() {
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
				GLFW_KEY_ENTER -> InputKey.ToggleMenu
				GLFW_KEY_TAB -> InputKey.ToggleMenu
				GLFW_KEY_ESCAPE -> InputKey.Escape
				GLFW_KEY_SPACE -> InputKey.Cheat
				GLFW_KEY_J -> InputKey.ScrollDown
				GLFW_KEY_K -> InputKey.ScrollUp
				else -> null
			}

			if (inputKey != null) {
				input.postEvent(
					InputKeyEvent(
						inputKey,
						didPress = action == GLFW_PRESS,
						didRelease = action == GLFW_RELEASE,
						didRepeat = action == GLFW_REPEAT
					)
				)
			}
		}

		glfwSetCursorPosCallback(glfwWindow) { _, x, y ->
			input.postEvent(MouseMoveEvent(x.roundToInt(), y.roundToInt()))
		}

		glfwSetMouseButtonCallback(glfwWindow) { _, button, action, _ ->
			if (button == GLFW_MOUSE_BUTTON_LEFT) input.postEvent(InputKeyEvent(
				InputKey.Click, action == GLFW_PRESS, false, action == GLFW_RELEASE
			))
		}

		for (jid in GLFW_JOYSTICK_1 .. GLFW_JOYSTICK_16) {
			if (glfwJoystickIsGamepad(jid)) joysticks.add(jid)
		}

		glfwSetJoystickCallback { jid, event ->
			if (event == GLFW_CONNECTED && glfwJoystickIsGamepad(jid)) joysticks.add(jid)
			if (event == GLFW_DISCONNECTED) joysticks.remove(jid)
		}
	}

	fun update() {
		var movingLeft = false
		var movingDown = false
		var movingRight = false
		var movingUp = false

		stackPush().use { stack ->
			val state = GLFWGamepadState.calloc(stack)
			for (jid in joysticks) {
				glfwGetGamepadState(jid, state)

				val axes = state.axes()
				val threshold = 0.8

				val leftX = axes[GLFW_GAMEPAD_AXIS_LEFT_X]
				val rightX = axes[GLFW_GAMEPAD_AXIS_RIGHT_X]
				if (leftX < -threshold || rightX < -threshold) movingLeft = true
				if (leftX > threshold || rightX > threshold) movingRight = true

				val leftY = axes[GLFW_GAMEPAD_AXIS_LEFT_Y]
				val rightY = axes[GLFW_GAMEPAD_AXIS_RIGHT_Y]
				if (leftY < -threshold || rightY < -threshold) movingUp = true
				if (leftY > threshold || rightY > threshold) movingDown = true
			}
		}

		postEvent(movingLeft, wasMovingLeft, InputKey.MoveLeft)
		postEvent(movingDown, wasMovingDown, InputKey.MoveDown)
		postEvent(movingRight, wasMovingRight, InputKey.MoveRight)
		postEvent(movingUp, wasMovingUp, InputKey.MoveUp)

		wasMovingLeft = movingLeft
		wasMovingDown = movingDown
		wasMovingRight = movingRight
		wasMovingUp = movingUp
	}

	private fun postEvent(newState: Boolean, oldState: Boolean, key: InputKey) {
		if (oldState != newState) {
			input.postEvent(InputKeyEvent(
				key = key,
				didPress = newState,
				didRepeat = false,
				didRelease = oldState
			))
		}
	}
}
