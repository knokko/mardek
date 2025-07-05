package mardek.game

import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import com.github.knokko.boiler.window.VkbWindow
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.renderer.BORDER_WIDTH
import mardek.renderer.FULL_BORDER_HEIGHT
import mardek.state.GameStateManager
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLGamepad.*
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32
import org.lwjgl.sdl.SDLSurface.SDL_CreateSurface
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.sdl.SDL_GamepadAxisEvent
import org.lwjgl.sdl.SDL_GamepadButtonEvent
import org.lwjgl.sdl.SDL_GamepadDeviceEvent
import org.lwjgl.sdl.SDL_KeyboardEvent
import org.lwjgl.sdl.SDL_MouseButtonEvent
import org.lwjgl.sdl.SDL_MouseMotionEvent
import org.lwjgl.sdl.SDL_Point
import org.lwjgl.sdl.SDL_WindowEvent
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memCalloc
import kotlin.math.abs

private const val iconScale = 4

private val iconData = memCalloc(4 * 16 * 16 * iconScale * iconScale).apply {
	val input = MardekSdlInput::class.java.getResource("icon.bin")!!
	val inputBytes = input.readBytes()
	for (ix in 0 until 16) {
		val ox = iconScale * ix
		for (iy in 0 until 16) {
			val oy = iconScale * iy
			val inputIndex = 4 * (ix + 16 * iy)
			for (sx in 0 until iconScale) {
				for (sy in 0 until iconScale) {
					for (i in 0 until 4) {
						val outputIndex = 4 * ( (ox + sx) + 16 * iconScale * (oy + sy)) + i
						put(outputIndex, inputBytes[inputIndex + i])
					}
				}
			}
		}
	}
	this
}

private const val TRIGGER_THRESHOLD = 10_000
private const val JOYSTICK_THRESHOLD = 10_000

class MardekSdlInput(
	private val window: VkbWindow,
	private val state: GameStateManager,
	private val input: InputManager
) {

	private var leftTrigger = 0
	private var rightTrigger = 0
	private var joystickX = 0
	private var joystickY = 0
	private var direction: InputKey? = null

	private fun updateJoystick() {
		var newDirection: InputKey? = null
		if (joystickX > JOYSTICK_THRESHOLD && joystickX > abs(joystickY)) newDirection = InputKey.MoveRight
		if (joystickX < -JOYSTICK_THRESHOLD && -joystickX > abs(joystickY)) newDirection = InputKey.MoveLeft
		if (joystickY > JOYSTICK_THRESHOLD && joystickY > abs(joystickX)) newDirection = InputKey.MoveDown
		if (joystickY < -JOYSTICK_THRESHOLD && -joystickY > abs(joystickX)) newDirection = InputKey.MoveUp

		if (direction != newDirection) {
			if (direction != null) {
				input.postEvent(InputKeyEvent(
					direction!!, didPress = false, didRepeat = false, didRelease = true
				))
			}
			if (newDirection != null) {
				input.postEvent(InputKeyEvent(
					newDirection, didPress = true, didRepeat = false, didRelease = false
				))
			}
		}
		direction = newDirection
	}

	fun register() {
		assertSdlSuccess(SDL_SetWindowMinimumSize(
			window.handle, 100, 100
		), "SetWindowMinimumSize")

		val gamepads = SDL_GetGamepads()
		assertSdlSuccess(gamepads != null, "GetGamepads")
		while (gamepads!!.hasRemaining()) {
			assertSdlSuccess(SDL_OpenGamepad(gamepads.get()) != 0L, "OpenGamepad")
		}

		stackPush().use { stack ->
			val windowIcon = SDL_CreateSurface(16 * iconScale, 16 * iconScale, SDL_PIXELFORMAT_RGBA32)
			assertSdlSuccess(windowIcon != null, "CreateSurface")
			windowIcon!!.pixels(iconData)
			assertSdlSuccess(SDL_SetWindowIcon(
				window.handle, windowIcon
			), "SetWindowIcon")
		}

		assertSdlSuccess(SDL_AddEventWatch({ userData, rawEvent ->
			val type = SDL_Event.ntype(rawEvent)
			if (type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP) {
				val key = when (SDL_KeyboardEvent.nkey(rawEvent)) {
					SDLK_LEFT -> InputKey.MoveLeft
					SDLK_A -> InputKey.MoveLeft
					SDLK_UP -> InputKey.MoveUp
					SDLK_W -> InputKey.MoveUp
					SDLK_RIGHT -> InputKey.MoveRight
					SDLK_D -> InputKey.MoveRight
					SDLK_DOWN -> InputKey.MoveDown
					SDLK_S -> InputKey.MoveDown
					SDLK_X -> InputKey.Interact
					SDLK_E -> InputKey.Interact
					SDLK_Z -> InputKey.Cancel
					SDLK_Q -> InputKey.Cancel
					SDLK_KP_ENTER -> InputKey.ToggleMenu
					SDLK_TAB -> InputKey.ToggleMenu
					SDLK_ESCAPE -> InputKey.Escape
					SDLK_SPACE -> InputKey.Cheat
					SDLK_J -> InputKey.ScrollDown
					SDLK_K -> InputKey.ScrollUp
					else -> null
				}

				if (key != null) {
					input.postEvent(InputKeyEvent(
						key,
						didPress = type == SDL_EVENT_KEY_DOWN,
						didRepeat = SDL_KeyboardEvent.nrepeat(rawEvent),
						didRelease = type == SDL_EVENT_KEY_UP
					))
				}
			}

			if (type == SDL_EVENT_MOUSE_MOTION) {
				val rawX = SDL_MouseMotionEvent.nx(rawEvent).toInt()
				val rawY = SDL_MouseMotionEvent.ny(rawEvent).toInt()
				input.postEvent(MouseMoveEvent(
					newX = rawX - BORDER_WIDTH,
					newY = rawY - FULL_BORDER_HEIGHT
				))

				val cross = state.crossLocation
				state.hoveringCross = cross != null && cross.contains(rawX, rawY)

				val maximize = state.maximizeLocation
				state.hoveringMaximize = maximize != null && maximize.contains(rawX, rawY)

				val minus = state.minusLocation
				state.hoveringMinus = minus != null && minus.contains(rawX, rawY)
			}

			if (type == SDL_EVENT_MOUSE_BUTTON_DOWN) {
				input.postEvent(InputKeyEvent(
					InputKey.Click, didPress = true, didRepeat = false, didRelease = false
				))

				val x = SDL_MouseButtonEvent.nx(rawEvent).toInt()
				val y = SDL_MouseButtonEvent.ny(rawEvent).toInt()

				val cross = state.crossLocation
				if (cross != null && cross.contains(x, y)) {
					stackPush().use { stack ->
						val quitEvent = SDL_Event.calloc(stack)
						quitEvent.type(SDL_EVENT_WINDOW_CLOSE_REQUESTED)
						SDL_WindowEvent.nwindowID(quitEvent.address(), SDL_GetWindowID(window.handle))
						assertSdlSuccess(SDL_PushEvent(quitEvent), "PushEvent")
					}
				}

				val maximize = state.maximizeLocation
				if (maximize != null && maximize.contains(x, y)) {
					if ((SDL_GetWindowFlags(window.handle) and SDL_WINDOW_MAXIMIZED) == 0L) {
						assertSdlSuccess(SDL_MaximizeWindow(window.handle), "MaximizeWindow")
					} else {
						assertSdlSuccess(SDL_RestoreWindow(window.handle), "RestoreWindow")
					}
				}

				val minus = state.minusLocation
				if (minus != null && minus.contains(x, y)) {
					assertSdlSuccess(SDL_MinimizeWindow(window.handle), "MinimizeWindow")
				}
			}
			if (type == SDL_EVENT_MOUSE_BUTTON_UP) {
				input.postEvent(InputKeyEvent(
					InputKey.Click, didPress = false, didRepeat = false, didRelease = true
				))
			}

			if (type == SDL_EVENT_GAMEPAD_ADDED) {
				val id = SDL_GamepadDeviceEvent.nwhich(rawEvent)
				assertSdlSuccess(SDL_OpenGamepad(id) != 0L, "OpenGamepad")
			}

			if (type == SDL_EVENT_GAMEPAD_BUTTON_DOWN || type == SDL_EVENT_GAMEPAD_BUTTON_UP) {
				val button = SDL_GamepadButtonEvent.nbutton(rawEvent).toInt()
				val key = when (button) {
					SDL_GAMEPAD_BUTTON_SOUTH -> InputKey.Interact
					SDL_GAMEPAD_BUTTON_WEST -> InputKey.Interact
					SDL_GAMEPAD_BUTTON_EAST -> InputKey.Cancel
					SDL_GAMEPAD_BUTTON_NORTH -> InputKey.ToggleMenu
					SDL_GAMEPAD_BUTTON_START -> InputKey.ToggleMenu
					SDL_GAMEPAD_BUTTON_GUIDE -> InputKey.ToggleMenu
					SDL_GAMEPAD_BUTTON_LEFT_SHOULDER -> InputKey.ToggleMenu
					SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER -> InputKey.ToggleMenu
					SDL_GAMEPAD_BUTTON_DPAD_LEFT -> InputKey.MoveLeft
					SDL_GAMEPAD_BUTTON_DPAD_UP -> InputKey.MoveUp
					SDL_GAMEPAD_BUTTON_DPAD_RIGHT -> InputKey.MoveRight
					SDL_GAMEPAD_BUTTON_DPAD_DOWN -> InputKey.MoveDown
					else -> null
				}

				if (key != null) {
					input.postEvent(InputKeyEvent(
						key,
						didPress = type == SDL_EVENT_GAMEPAD_BUTTON_DOWN,
						didRepeat = false,
						didRelease = type == SDL_EVENT_GAMEPAD_BUTTON_UP
					))
				}
			}

			if (type == SDL_EVENT_GAMEPAD_AXIS_MOTION) {
				val axis = SDL_GamepadAxisEvent.naxis(rawEvent).toInt()
				val amount = SDL_GamepadAxisEvent.nvalue(rawEvent)

				if (axis == SDL_GAMEPAD_AXIS_LEFT_TRIGGER) {
					if (leftTrigger < TRIGGER_THRESHOLD && amount >= TRIGGER_THRESHOLD) {
						input.postEvent(InputKeyEvent(
							InputKey.ToggleMenu, didPress = true, didRepeat = false, false
						))
					}
					if (leftTrigger >= TRIGGER_THRESHOLD && amount < TRIGGER_THRESHOLD) {
						input.postEvent(InputKeyEvent(
							InputKey.ToggleMenu, didPress = false, didRepeat = false, true
						))
					}
					leftTrigger = amount.toInt()
				}

				if (axis == SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) {
					if (rightTrigger < TRIGGER_THRESHOLD && amount >= TRIGGER_THRESHOLD) {
						input.postEvent(InputKeyEvent(
							InputKey.ToggleMenu, didPress = true, didRepeat = false, false
						))
					}
					if (rightTrigger >= TRIGGER_THRESHOLD && amount < TRIGGER_THRESHOLD) {
						input.postEvent(InputKeyEvent(
							InputKey.ToggleMenu, didPress = false, didRepeat = false, true
						))
					}
					rightTrigger = amount.toInt()
				}

				if (axis == SDL_GAMEPAD_AXIS_LEFTX) {
					joystickX = amount.toInt()
					updateJoystick()
				}

				if (axis == SDL_GAMEPAD_AXIS_LEFTY) {
					joystickY = amount.toInt()
					updateJoystick()
				}
			}

			false
		}, 0L), "AddEventWatch")

		assertSdlSuccess(SDL_SetWindowHitTest(window.handle, { handle, rawPoint, userData ->
			val x = SDL_Point.nx(rawPoint)
			val y = SDL_Point.ny(rawPoint)

			val resizeWidth = 5
			if (y >= window.height - resizeWidth) {
				if (x < resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_BOTTOMLEFT
				if (x >= window.width - resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_BOTTOMRIGHT
				return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_BOTTOM
			}

			if (y < 3) {
				if (x < resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_TOPLEFT
				if (x >= window.width - resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_TOPRIGHT
				return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_TOP
			}

			if (x < resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_LEFT
			if (x >= window.width - resizeWidth) return@SDL_SetWindowHitTest SDL_HITTEST_RESIZE_RIGHT

			if (y < FULL_BORDER_HEIGHT) {
				for (icon in arrayOf(state.crossLocation, state.maximizeLocation, state.minusLocation)) {
					if (icon != null && icon.contains(x, y)) {
						return@SDL_SetWindowHitTest SDL_HITTEST_NORMAL
					}
				}

				return@SDL_SetWindowHitTest SDL_HITTEST_DRAGGABLE
			}

			SDL_HITTEST_NORMAL
		}, 0L), "SetWindowHintTest")
	}

	fun update() {}
}
