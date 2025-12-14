package mardek.game

import com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess
import com.github.knokko.boiler.window.VkbWindow
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.input.TextTypeEvent
import mardek.renderer.FULL_BORDER_HEIGHT
import mardek.renderer.MardekCursor
import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.title.TitleScreenState
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLGamepad.*
import org.lwjgl.sdl.SDLKeyboard.SDL_StartTextInput
import org.lwjgl.sdl.SDLKeyboard.SDL_StopTextInput
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLMouse.SDL_CreateColorCursor
import org.lwjgl.sdl.SDLMouse.SDL_HideCursor
import org.lwjgl.sdl.SDLMouse.SDL_SetCursor
import org.lwjgl.sdl.SDLMouse.SDL_ShowCursor
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
import org.lwjgl.sdl.SDL_TextInputEvent
import org.lwjgl.sdl.SDL_WindowEvent
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memCalloc
import java.nio.ByteBuffer
import kotlin.math.abs

private const val WINDOW_ICON_SCALE = 4
private const val CURSOR_SCALE = 2

private val rawIcons = run {
	val inputBytes = MardekSdlInput::class.java.getResource("icons.bin")!!.readBytes()

	fun createScaled(startIndex: Int, scale: Int): ByteBuffer {
		val destination = memCalloc(4 * 16 * 16 * scale * scale)
		for (ix in 0 until 16) {
			val ox = scale * ix
			for (iy in 0 until 16) {
				val oy = scale * iy
				val inputIndex = startIndex + 4 * (ix + 16 * iy)
				for (sx in 0 until scale) {
					for (sy in 0 until scale) {
						for (i in 0 until 4) {
							val outputIndex = 4 * ((ox + sx) + 16 * scale * (oy + sy)) + i
							destination.put(outputIndex, inputBytes[inputIndex + i])
						}
					}
				}
			}
		}
		return destination
	}

	val windowIcon = createScaled(0, 4)

	val cursorIcons = MardekCursor.entries.map {
		val baseByteSize = 4 * 16 * 16
		createScaled(baseByteSize * (1 + it.ordinal), CURSOR_SCALE)
	}

	arrayOf(windowIcon) + cursorIcons
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

	private var desiredCursor = MardekCursor.Pointer
	private var shownCursor: MardekCursor? = null
	private var lastCursorAction = System.nanoTime()
	private var isTextInputEnabled = false

	private val cursors = LongArray(MardekCursor.entries.size)

	private fun shouldShowCursor() = desiredCursor == MardekCursor.Grab ||
			System.nanoTime() - lastCursorAction <= 2000_000_000L

	fun updateCursor(desired: MardekCursor) {
		this.desiredCursor = desired
		val cursorToShow = if (shouldShowCursor()) this.desiredCursor else null
		if (cursorToShow != shownCursor) {
			if (cursorToShow == null) {
				assertSdlSuccess(SDL_HideCursor(), "HideCursor")
			} else {
				assertSdlSuccess(SDL_ShowCursor(), "ShowCursor")
				assertSdlSuccess(SDL_SetCursor(cursors[cursorToShow.ordinal]), "SetCursor")
			}
			shownCursor = cursorToShow
		}
	}

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
			window.properties.handle, 100, 100
		), "SetWindowMinimumSize")

		val gamepads = SDL_GetGamepads()
		assertSdlSuccess(gamepads != null, "GetGamepads")
		while (gamepads!!.hasRemaining()) {
			assertSdlSuccess(SDL_OpenGamepad(gamepads.get()) != 0L, "OpenGamepad")
		}

		val windowIcon = SDL_CreateSurface(
			16 * WINDOW_ICON_SCALE,
			16 * WINDOW_ICON_SCALE,
			SDL_PIXELFORMAT_RGBA32
		)
		assertSdlSuccess(windowIcon != null, "CreateSurface")
		windowIcon!!.pixels(rawIcons[0])

		// Note: don't use assertSdlSuccess because Wayland does not always support window icons
		SDL_SetWindowIcon(window.properties.handle, windowIcon)

		for (cursorType in MardekCursor.entries) {
			val cursorIcon = SDL_CreateSurface(
				16 * CURSOR_SCALE,
				16 * CURSOR_SCALE,
				SDL_PIXELFORMAT_RGBA32
			)
			assertSdlSuccess(cursorIcon != null, "CreateSurface")
			cursorIcon!!.pixels(rawIcons[1 + cursorType.ordinal])

			val cursor = SDL_CreateColorCursor(
				cursorIcon, 2 * CURSOR_SCALE, 2 * CURSOR_SCALE
			)
			assertSdlSuccess(cursor != 0L, "CreateColorCursor")
			cursors[cursorType.ordinal] = cursor
		}

		assertSdlSuccess(SDL_AddEventWatch({ _, rawEvent ->
			val type = SDL_Event.ntype(rawEvent)
			if (type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP) {
				val modifierKey = SDL_KeyboardEvent.nmod(rawEvent).toInt()
				val pressedKey = SDL_KeyboardEvent.nkey(rawEvent)
				val holdsControl = (SDL_KMOD_CTRL and modifierKey) != 0
				val key = if (holdsControl) {
					when (pressedKey) {
						SDLK_J -> InputKey.CheatScrollDown
						SDLK_K -> InputKey.CheatScrollUp
						SDLK_S -> InputKey.CheatSave
						else -> null
					}
				} else {
					when (pressedKey) {
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
						SDLK_RETURN -> InputKey.ToggleMenu
						SDLK_TAB -> InputKey.ToggleMenu
						SDLK_ESCAPE -> InputKey.Escape
						SDLK_SPACE -> InputKey.CheatMove
						SDLK_BACKSPACE -> InputKey.BackspaceLast
						SDLK_DELETE -> InputKey.BackspaceFirst
						else -> null
					}
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

			if (type == SDL_EVENT_TEXT_INPUT) {
				val typedText = SDL_TextInputEvent.ntextString(rawEvent)
				if (typedText != null) input.postEvent(TextTypeEvent(typedText))
			}

			if (type == SDL_EVENT_MOUSE_MOTION) {
				lastCursorAction = System.nanoTime()

				val newX = SDL_MouseMotionEvent.nx(rawEvent).toInt()
				val newY = SDL_MouseMotionEvent.ny(rawEvent).toInt()
				input.postEvent(MouseMoveEvent(newX, newY))

				val cross = state.crossLocation
				state.hoveringCross = cross != null && cross.contains(newX, newY)

				val maximize = state.maximizeLocation
				state.hoveringMaximize = maximize != null && maximize.contains(newX, newY)

				val minus = state.minusLocation
				state.hoveringMinus = minus != null && minus.contains(newX, newY)
			}

			if (type == SDL_EVENT_MOUSE_BUTTON_DOWN) {
				lastCursorAction = System.nanoTime()
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
						SDL_WindowEvent.nwindowID(quitEvent.address(), SDL_GetWindowID(window.properties.handle))
						assertSdlSuccess(SDL_PushEvent(quitEvent), "PushEvent")
					}
				}

				val maximize = state.maximizeLocation
				if (maximize != null && maximize.contains(x, y)) {
					if ((SDL_GetWindowFlags(window.properties.handle) and SDL_WINDOW_MAXIMIZED) == 0L) {
						assertSdlSuccess(SDL_MaximizeWindow(window.properties.handle), "MaximizeWindow")
					} else {
						assertSdlSuccess(SDL_RestoreWindow(window.properties.handle), "RestoreWindow")
					}
				}

				val minus = state.minusLocation
				if (minus != null && minus.contains(x, y)) {
					assertSdlSuccess(SDL_MinimizeWindow(window.properties.handle), "MinimizeWindow")
				}
			}
			if (type == SDL_EVENT_MOUSE_BUTTON_UP) {
				lastCursorAction = System.nanoTime()
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

		assertSdlSuccess(SDL_SetWindowHitTest(window.properties.handle, { _, rawPoint, _ ->
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

	fun update() {
		val currentState = state.currentState
		var newCursor = MardekCursor.Pointer

		if (currentState is InGameState) {
			val currentTab = currentState.menu.currentTab
			if (currentTab is InventoryTab) {
				newCursor = MardekCursor.Inventory
				if (currentTab.pickedUpItem != null) newCursor = MardekCursor.Grab
			}
		}

		val shouldHaveTextInput = currentState is TitleScreenState && currentState.newCampaignName != null
		if (shouldHaveTextInput && !isTextInputEnabled) {
			assertSdlSuccess(SDL_StartTextInput(window.properties.handle), "StartTextInput")
			isTextInputEnabled = true
		}
		if (!shouldHaveTextInput && isTextInputEnabled) {
			assertSdlSuccess(SDL_StopTextInput(window.properties.handle), "StopTextInput")
			isTextInputEnabled = false
		}

		updateCursor(newCursor)
	}
}
